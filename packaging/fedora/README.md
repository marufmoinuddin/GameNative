# Fedora Packaging Scaffold

This directory contains the initial RPM packaging scaffold for Linux ARM64:

- gamenative.spec
- gamenative.desktop
- gamenative.appdata.xml
- deps.txt
- post-install.sh

Build precondition checks:

- ./gradlew -p gamenative-linux :core:runtime:runRuntimeProof
- ./gradlew -p gamenative-linux :core:runtime:test :desktop:shell:test
