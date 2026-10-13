#!/usr/bin/env bash
# Limpieza de /implement tras el merge: borra worktree, rama local y rama remota de las PRs mergeadas.
# Uso: cleanup.sh [<issue>]   (sin argumento revisa todas las ramas <issue>-*)
# Salida: 0 = correcto, 1 = la issue indicada no se puede limpiar, 2 = error de uso o de entorno.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$DIR/lib.sh"

[[ $# -le 1 ]] || { echo "Uso: cleanup.sh [<issue>]" >&2; exit 2; }
N=${1:-}
[[ -z $N || $N =~ ^[0-9]+$ ]] || { echo "Uso: cleanup.sh [<issue>]" >&2; exit 2; }

requiere git gh
requiere_gh
ORIGEN=$PWD
PRINCIPAL=$(checkout_principal)
cd "$PRINCIPAL"
git fetch origin --prune --quiet \
  || error_entorno "No se ha podido actualizar origin; comprueba la conexión."

no_procede() {
  if [[ -n $N ]]; then fallo "$*"; else aviso "$*"; fi
}

PATRON=${N:-[0-9]*}
RAMAS=$(git for-each-ref --format='%(refname:short)' "refs/heads/$PATRON-*" | grep -E '^[0-9]+-' | sort || true)
if [[ -z $RAMAS ]]; then
  ok "No hay ramas locales${N:+ de la issue #$N} que limpiar."
fi

for RAMA in $RAMAS; do
  WT=$(worktrees_con_rama | awk -F'\t' -v r="$RAMA" '$2 == r { print $1 }')
  PR=$(gh pr list --state all --head "$RAMA" --limit 1 --json number,state,headRefOid \
    -q '.[0] | select(. != null) | "\(.number) \(.state) \(.headRefOid)"')
  if [[ -z $PR ]]; then
    no_procede "La rama $RAMA no tiene PR, así que no se borra."
    continue
  fi
  read -r NUM ESTADO OID <<<"$PR"
  if [[ $ESTADO != MERGED ]]; then
    no_procede "La PR #$NUM de la rama $RAMA está $ESTADO y no mergeada, así que no se borra."
    continue
  fi
  PUNTA=$(git rev-parse "refs/heads/$RAMA")
  if [[ $PUNTA != "$OID" ]]; then
    no_procede "La rama $RAMA (${PUNTA:0:7}) no coincide con el último commit de la PR #$NUM (${OID:0:7}); puede haber trabajo sin subir, así que no se borra."
    continue
  fi
  if [[ $WT == "$PRINCIPAL" ]]; then
    no_procede "La rama $RAMA está en el checkout principal; cambia de rama antes de limpiarla."
    continue
  fi
  if [[ -n $WT ]]; then
    case $ORIGEN/ in "$WT"/*) no_procede "Se está ejecutando desde dentro de $WT; hazlo desde otro directorio."; continue ;; esac
    if [[ -n $(git -C "$WT" status --porcelain) ]]; then
      no_procede "El worktree $WT tiene cambios sin commitear, así que no se borra."
      continue
    fi
    git worktree remove "$WT"
  fi
  git branch -D --quiet "$RAMA"
  if git ls-remote --exit-code --heads origin "$RAMA" >/dev/null 2>&1; then
    git push --quiet origin --delete "$RAMA"
    REMOTA="y la rama remota"
  else
    REMOTA="(la rama remota ya no existía)"
  fi
  ok "PR #$NUM mergeada: borrados ${WT:+el worktree $WT, }la rama local $RAMA $REMOTA."
done

git worktree prune
git fetch origin --prune --quiet
terminar
