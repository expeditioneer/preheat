{
  description = "Preheat — Android development environment (API 36 / Android 16, JDK 17)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            android_sdk.accept_license = true;
            allowUnfree = true;
          };
        };

        androidComposition = pkgs.androidenv.composeAndroidPackages {
          platformVersions = [ "36" "35" ];
          buildToolsVersions = [ "36.0.0" "35.0.0" ];
          platformToolsVersion = "35.0.2";
          includeEmulator = false;
          includeSystemImages = false;
          includeSources = false;
          includeNDK = false;
        };

        androidSdk = androidComposition.androidsdk;
        androidSdkRoot = "${androidSdk}/libexec/android-sdk";

        jdk = pkgs.jdk17;
      in
      {
        devShells.default = pkgs.mkShell {
          packages = [
            jdk
            androidSdk
            pkgs.android-tools
          ];

          ANDROID_HOME = androidSdkRoot;
          ANDROID_SDK_ROOT = androidSdkRoot;
          JAVA_HOME = "${jdk}/lib/openjdk";

          shellHook = ''
            export ANDROID_USER_HOME="$PWD/.android"
            export GRADLE_USER_HOME="$PWD/.gradle"
            mkdir -p "$ANDROID_USER_HOME" "$GRADLE_USER_HOME"
            echo "Preheat devshell"
            echo "  JDK:         $(java -version 2>&1 | head -1)"
            echo "  ANDROID_SDK: $ANDROID_SDK_ROOT"
            echo "  ANDROID_USER_HOME: $ANDROID_USER_HOME"
            echo "  GRADLE_USER_HOME: $GRADLE_USER_HOME"
            echo "  Build:       ./gradlew assembleDebug"
            echo "  Lint:        ./gradlew lint"
          '';
        };

        formatter = pkgs.nixpkgs-fmt;
      });
}
