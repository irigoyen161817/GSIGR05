#!/usr/bin/env bash
# Comprobaciones previas de /implement (paso 2).
# Uso: precheck.sh <issue> [--stack <rama-base>]
# Salida: 0 = se puede empezar, 1 = alguna comprobación bloqueante falla, 2 = error de uso o de entorno.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$DIR/lib.sh"

uso() {
  echo "Uso: precheck.sh <issue> [--stack <rama-base>]" >&2
  exit 2
}

[[ $# -ge 1 && $1 =~ ^[0-9]+$ ]] || uso
N=$1
shift
STACK=""
while (($# > 0)); do
  case $1 in
    --stack) [[ $# -ge 2 ]] || uso; STACK=$2; shift 2 ;;
    *) uso ;;
  esac
done

requiere git gh
requiere_gh
REPO=$(repo_actual)
YO=$(gh api user -q .login)
git fetch origin --prune --quiet \
  || error_entorno "No se ha podido actualizar origin; comprueba la conexión."

ESTADO=$(gh issue view "$N" --json state -q .state 2>/dev/null) \
  || error_entorno "No se encuentra la issue #$N en $REPO."
CUERPO=$(gh issue view "$N" --json body -q .body)

if [[ $ESTADO == OPEN ]]; then
  ok "La issue #$N está abierta."
else
  fallo "La issue #$N está cerrada, así que no hay nada que implementar."
fi

DEPENDENCIAS=$(printf '%s\n' "$CUERPO" | awk '/^##+ /{ dentro = tolower($0) ~ /dependencias/; next } dentro' | grep -oE '#[0-9]+' | tr -d '#' | grep -vx "$N" | sort -un || true)
if [[ -z $DEPENDENCIAS ]]; then
  ok "La issue #$N no declara dependencias en su sección ## Dependencias."
fi
for M in $DEPENDENCIAS; do
  read -r EST RAZON < <(gh issue view "$M" --json state,stateReason -q '"\(.state) \(.stateReason)"')
  if [[ $EST == CLOSED && $RAZON == COMPLETED ]]; then
    ok "La dependencia #$M está cerrada y completada."
  elif [[ $EST == CLOSED ]]; then
    fallo "La dependencia #$M se cerró sin completarse, así que lo que se necesita de ella no está en main."
  elif [[ -n $STACK ]]; then
    aviso "La dependencia #$M sigue abierta; se permite porque se trabaja en stack sobre \"$STACK\"."
  else
    fallo "La dependencia #$M sigue abierta; hay que esperar a que se mergee o trabajar en stack con --stack <rama>."
  fi
done

ASIGNADOS=$(gh issue view "$N" --json assignees -q '.assignees[].login' | sort)
OTROS=$(printf '%s\n' "$ASIGNADOS" | grep -vxF "$YO" | grep -v '^$' || true)
if [[ -n $OTROS ]]; then
  fallo "La issue #$N está asignada a $(echo "$OTROS" | paste -sd, -), así que no se puede coger sin su permiso."
elif [[ -n $ASIGNADOS ]]; then
  ok "La issue #$N ya está asignada a ti ($YO)."
else
  ok "La issue #$N no está asignada a nadie."
fi

DUPLICADO=0
while IFS=$'\t' read -r RUTA RAMA; do
  [[ $RAMA == "$N"-* ]] || continue
  fallo "Ya existe el worktree $RUTA con la rama $RAMA para la issue #$N; hay que retomarlo en vez de crear otro."
  DUPLICADO=1
done < <(worktrees_con_rama)
while read -r RAMA; do
  [[ -n $RAMA ]] || continue
  fallo "Ya existe la rama $RAMA para la issue #$N."
  DUPLICADO=1
done < <(git for-each-ref --format='%(refname:short)' "refs/heads/$N-*" "refs/remotes/origin/$N-*" | sort)
while IFS=$'\t' read -r PR RAMA AUTOR; do
  [[ -n $PR ]] || continue
  fallo "Ya hay una PR abierta para la issue #$N: #$PR ($RAMA, de @$AUTOR)."
  DUPLICADO=1
done < <(gh pr list --state open --limit 200 --json number,title,headRefName,author,closingIssuesReferences \
  -q ".[] | select((.headRefName | startswith(\"$N-\")) or (.title | startswith(\"[#$N]\")) or any(.closingIssuesReferences[]; .number == $N)) | \"\(.number)\t\(.headRefName)\t\(.author.login)\"" | sort -n)
if ((DUPLICADO == 0)); then
  ok "No hay worktree, rama ni PR abierta para la issue #$N."
fi

SOLAPES=$(
  # shellcheck disable=SC2016
  gh pr list --state open --limit 200 --json number,author,files \
    -q '.[] | .number as $n | .author.login as $a | .files[].path | "PR #\($n) de @\($a): \(.)"'
  while IFS=$'\t' read -r RUTA RAMA; do
    [[ $RUTA != "$(checkout_principal)" ]] || continue
    {
      git -C "$RUTA" diff --name-only origin/main...HEAD
      git -C "$RUTA" status --porcelain -uall | cut -c4-
    } | sed "s|^|worktree $RUTA ($RAMA): |"
  done < <(worktrees_con_rama)
)
if [[ -n $SOLAPES ]]; then
  printf '%s\n' "$SOLAPES" | sort -u | while read -r LINEA; do
    info "Fichero en curso en $LINEA"
  done
else
  info "No hay PRs abiertas ni worktrees con cambios; no puede haber solapes."
fi

while IFS=$'\t' read -r RUTA RAMA; do
  [[ $RAMA =~ ^([0-9]+)- ]] || continue
  if [[ $(gh pr list --state merged --head "$RAMA" --json number -q length) -gt 0 ]]; then
    aviso "La PR de la rama $RAMA ya está mergeada; limpia su worktree con cleanup.sh ${BASH_REMATCH[1]}."
  fi
done < <(worktrees_con_rama)

EPICAS=$(
  gh api "repos/$REPO/issues/$N/parent" -q .number 2>/dev/null | grep -E '^[0-9]+$' || true
  printf '%s\n' "$CUERPO" | grep -oiE 'parte de #[0-9]+' | grep -oE '[0-9]+' || true
)
EPICAS=$(printf '%s\n' "$EPICAS" | grep -v '^$' | sort -un || true)
SUBISSUES=$(
  gh api "repos/$REPO/issues/$N/sub_issues" -q '.[].number' 2>/dev/null | grep -E '^[0-9]+$' || true
  printf '%s\n' "$CUERPO" | grep -oE '^[[:space:]]*- \[[ xX]\] #[0-9]+' | grep -oE '[0-9]+$' || true
)
SUBISSUES=$(printf '%s\n' "$SUBISSUES" | grep -v '^$' | sort -un || true)
lista() {
  if [[ -n $1 ]]; then printf '%s\n' "$1" | sed 's/^/#/' | paste -sd' ' -; else echo ninguna; fi
}
info "Épica: $(lista "$EPICAS")"
info "Sub-issues: $(lista "$SUBISSUES")"

terminar
