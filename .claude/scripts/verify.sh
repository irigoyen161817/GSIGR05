#!/usr/bin/env bash
# Validaciones finales de /implement antes del push (paso 6).
# Uso: verify.sh [<ruta-worktree>] [--base <rama>]
# Salida: 0 = todo correcto, 1 = alguna validación falla, 2 = error de uso o de entorno.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$DIR/lib.sh"

uso() {
  echo "Uso: verify.sh [<ruta-worktree>] [--base <rama>]" >&2
  exit 2
}

RUTA=.
BASE=main
while (($# > 0)); do
  case $1 in
    --base) [[ $# -ge 2 ]] || uso; BASE=$2; shift 2 ;;
    -*) uso ;;
    *) RUTA=$1; shift ;;
  esac
done

requiere git ant java
RAIZ=$(git -C "$RUTA" rev-parse --show-toplevel 2>/dev/null) \
  || error_entorno "\"$RUTA\" no es un repositorio git."
cd "$RAIZ"
git fetch origin "$BASE" --quiet \
  || error_entorno "No se ha podido descargar origin/$BASE; comprueba la conexión y el nombre de la rama."

PROVISTOS='GSIGR05/src/GSILabs/BSystem/(LeisureOffice|LookupService)\.java'
ATRIBUCION='co-authored-by:|generated with|claude\.ai/|anthropic\.com|🤖'
PROHIBIDOS='(^|/)(build|dist)/|(^|/)nbproject/private/|\.pdf$|\.ods$'

RAMA=$(git rev-parse --abbrev-ref HEAD)
if [[ $RAMA =~ ^([0-9]+)- ]]; then
  N=${BASH_REMATCH[1]}
  PATRON_ASUNTO="^\[#$N\] - .+"
else
  N=""
  PATRON_ASUNTO='^\[#[0-9]+\] - .+'
  aviso "La rama $RAMA no empieza por <issue>-, así que solo se comprueba que los commits citen alguna issue."
fi

COMMITS=$(git rev-list --reverse "origin/$BASE..HEAD")
if [[ -z $COMMITS ]]; then
  aviso "No hay commits nuevos respecto a origin/$BASE."
else
  TOTAL=$(echo "$COMMITS" | wc -l)
  MAL_FORMATO=0
  CON_ATRIBUCION=0
  for C in $COMMITS; do
    ASUNTO=$(git log -1 --format=%s "$C")
    if ! [[ $ASUNTO =~ $PATRON_ASUNTO ]]; then
      fallo "El commit ${C:0:7} \"$ASUNTO\" no sigue el formato [#${N:-N}] - Descripción."
      MAL_FORMATO=1
    fi
    LINEAS=$(git log -1 --format=%B "$C" | grep -iE "$ATRIBUCION" || true)
    if [[ -n $LINEAS ]]; then
      fallo "El commit ${C:0:7} \"$ASUNTO\" incluye una atribución o enlace de sesión prohibido: $(echo "$LINEAS" | paste -sd'|' -)"
      CON_ATRIBUCION=1
    fi
  done
  ((MAL_FORMATO == 1)) || ok "Los $TOTAL commits siguen el formato [#${N:-N}] - Descripción."
  ((CON_ATRIBUCION == 1)) || ok "Ningún commit incluye atribuciones ni enlaces de sesión."
fi

PROHIBIDOS_DIFF=$(git diff --name-only "origin/$BASE...HEAD" | grep -E "$PROHIBIDOS" | sort || true)
if [[ -n $PROHIBIDOS_DIFF ]]; then
  while read -r F; do
    fallo "El fichero $F no debe subirse: es generado o es un PDF."
  done <<<"$PROHIBIDOS_DIFF"
else
  ok "No se suben ficheros generados (build/, dist/, nbproject/private/, .ods) ni PDFs."
fi

SIN_COMMIT=$(git status --porcelain -uall | cut -c4- | sort)
if [[ -n $SIN_COMMIT ]]; then
  aviso "Hay cambios sin commitear que no se validan en la PR: $(echo "$SIN_COMMIT" | paste -sd, -)"
fi

LOG=$(mktemp)
trap 'rm -f "$LOG"' EXIT

if ant -f GSIGR05/build.xml clean jar >"$LOG" 2>&1; then
  ok "El proyecto compila y se empaqueta (ant clean jar)."
  COMPILA=1
else
  fallo "El proyecto no compila (ant clean jar):"
  grep -E 'error:|BUILD FAILED' "$LOG" | head -20 | sed 's/^/         /'
  COMPILA=0
fi

if ((COMPILA == 1)); then
  if ant -f GSIGR05/build.xml javadoc >"$LOG" 2>&1; then
    WARNINGS=$(grep -oE 'GSIGR05/src/[^:]+\.java:[0-9]+: warning: .*' "$LOG" | sort -u || true)
    AJENOS=$(echo "$WARNINGS" | grep -E "^$PROVISTOS:" || true)
    PROPIOS=$(echo "$WARNINGS" | grep -vE "^$PROVISTOS:" | grep -v '^$' || true)
    if [[ -n $PROPIOS ]]; then
      while read -r W; do
        fallo "Warning de Javadoc: $W"
      done <<<"$PROPIOS"
    else
      ok "El Javadoc se genera sin warnings en el código propio."
    fi
    if [[ -n $AJENOS ]]; then
      aviso "$(echo "$AJENOS" | wc -l) warnings de Javadoc en LeisureOffice/LookupService, que se copian de MiAulario sin alterar."
    fi
  else
    fallo "El Javadoc no se genera (ant javadoc):"
    grep -E 'error:|BUILD FAILED' "$LOG" | head -20 | sed 's/^/         /'
  fi

  if java -jar GSIGR05/dist/GSIGR05.jar >"$LOG" 2>&1; then
    ok "La clase principal termina con código 0. $(grep -E 'Correctas: [0-9]+' "$LOG" | tail -1 || true)"
  else
    fallo "La clase principal termina con error:"
    grep -E 'FALLO|Exception|Correctas:' "$LOG" | head -20 | sed 's/^/         /'
  fi
else
  fallo "No se generan el Javadoc ni se ejecuta la clase principal porque el proyecto no compila."
fi

terminar
