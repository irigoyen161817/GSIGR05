# shellcheck shell=bash
# Funciones comunes de los scripts de /implement. Se carga con `source`.

FALLOS=0

ok()    { printf 'OK     %s\n' "$*"; }
aviso() { printf 'AVISO  %s\n' "$*"; }
info()  { printf 'INFO   %s\n' "$*"; }
fallo() { printf 'FALLO  %s\n' "$*"; FALLOS=$((FALLOS + 1)); }

error_entorno() {
  printf 'ERROR  %s\n' "$*" >&2
  exit 2
}

requiere() {
  local c
  for c in "$@"; do
    command -v "$c" >/dev/null 2>&1 \
      || error_entorno "Falta el comando \"$c\" y sin él no se pueden hacer las comprobaciones."
  done
}

requiere_gh() {
  gh auth status >/dev/null 2>&1 \
    || error_entorno "gh no tiene una sesión iniciada; ejecuta \"gh auth login\" y vuelve a intentarlo."
}

repo_actual() {
  gh repo view --json nameWithOwner -q .nameWithOwner
}

checkout_principal() {
  git worktree list --porcelain | sed -n '1s/^worktree //p'
}

worktrees_con_rama() {
  git worktree list --porcelain | awk '
    /^worktree / { ruta = substr($0, 10) }
    /^branch refs\/heads\// { print ruta "\t" substr($0, 19) }
  ' | sort
}

terminar() {
  if ((FALLOS > 0)); then
    printf '\n%d comprobación(es) fallida(s).\n' "$FALLOS"
    exit 1
  fi
  printf '\nTodo correcto.\n'
  exit 0
}
