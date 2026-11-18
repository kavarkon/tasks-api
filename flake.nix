{
  description = "tasks api";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }:
    flake-utils.lib.eachDefaultSystem (
      system: let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [];
        };
        javaVersion = 21;
        jdk = pkgs.jdk21;
      in {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdk
            gradle
            spring-boot-cli
            jdt-language-server
            lombok
          ];

          shellHook = ''
            export JAVA_HOME=${jdk}
            export PATH="${jdk}/bin:$PATH"
            export GRADLE_OPTS="-Dorg.gradle.java.home=${jdk}"
            export DATABASE_URL="jdbc:postgresql://localhost/tasks?user=postgres&password=password"
            export JDTLS_JVM_ARGS="-javaagent:${pkgs.lombok}/share/java/lombok.jar"
            if [ -z "''${DOCKER_HOST+x}" ] && [ -S "''${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/podman/podman.sock" ]; then
              export DOCKER_HOST="unix://''${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/podman/podman.sock"
            fi

            echo "Tasks API development environment"
            echo "JDK version: ${toString javaVersion}"
            echo "JDK path: $JAVA_HOME"
            java --version
            gradle --version

          '';
        };

        packages.default = pkgs.stdenv.mkDerivation {
          pname = "spring-tasks-api";
          version = "0.1.0";
          src = ./.;

          buildInputs = with pkgs; [
            jdk
            gradle
          ];

          buildPhase = ''
            export JAVA_HOME=${jdk}
            export PATH="${jdk}/bin:$PATH"
            ./gradlew assemble
          '';

          installPhase = ''
            mkdir -p $out/share/java
            cp build/libs/*.jar $out/share/java/
          '';
        };
      }
    );
}
