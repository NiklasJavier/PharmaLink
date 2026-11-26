{
  description = "PharmaLink devShell — reproducible Java 21 + Java 11, Gradle, just, and validation tooling";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs =
    { self, nixpkgs }:
    let
      systems = [
        "aarch64-darwin"
        "x86_64-darwin"
        "aarch64-linux"
        "x86_64-linux"
      ];
      forAllSystems =
        f:
        nixpkgs.lib.genAttrs systems (
          system:
          f (
            import nixpkgs {
              inherit system;
            }
          )
        );
    in
    {
      devShells = forAllSystems (
        pkgs: {
          default = pkgs.mkShell {
            name = "pharmalink-dev";
            packages = with pkgs; [
              just
              jdk21
              jdk11
              git
              curl
              jq
              zip
              unzip
              python3
              ruby
              shellcheck
            ];
            shellHook = ''
              resolve_jdk_home() {
                local base="$1"
                for candidate in "$base"/Library/Java/JavaVirtualMachines/*/Contents/Home "$base"/Contents/Home; do
                  if [ -x "$candidate/bin/javac" ]; then
                    printf '%s\n' "$candidate"
                    return 0
                  fi
                done
                if [ -x "$base/bin/javac" ]; then
                  printf '%s\n' "$base"
                  return 0
                fi
                printf '%s\n' "$base"
              }

              export JDK21_HOME="$(resolve_jdk_home "${pkgs.jdk21.home}")"
              export JDK11_HOME="$(resolve_jdk_home "${pkgs.jdk11.home}")"
              export JAVA_HOME="$JDK21_HOME"
              export PATH="$JDK21_HOME/bin:$PATH"
              export ORG_GRADLE_PROJECT_org_gradle_java_installations_fromEnv="JDK21_HOME,JDK11_HOME"
              export ORG_GRADLE_PROJECT_org_gradle_java_installations_paths="$JDK21_HOME,$JDK11_HOME"

              if [ -t 1 ]; then
                echo "PharmaLink devShell · Java 21 + Java 11 · run: just --list"
              fi
            '';
          };
        }
      );

      formatter = forAllSystems (pkgs: pkgs.nixfmt-rfc-style);
    };
}
