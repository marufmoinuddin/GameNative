%global app_version 0.2.0
%global debug_package %{nil}

Name:           gamenative
Version:        %{app_version}
Release:        1%{?dist}
Summary:        GameNative Linux launcher and CLI for ARM64

License:        Apache-2.0
URL:            https://github.com/maruf/GameNative
Source0:        %{url}/releases/download/v%{version}/%{name}-%{version}-aarch64.tar.gz

ExclusiveArch:  aarch64

Requires:       wine
Requires:       box64
Requires:       mesa-vulkan-drivers
Requires:       libsecret
Requires:       java-headless

%description
GameNative desktop launcher, interactive CLI, and runtime orchestration stack
for Linux ARM64. Supports Steam login with 2FA, game library browsing,
download management, and Wine/Box64 game launch.

%prep
%setup -q -c -T
tar -xzf %{SOURCE0}

%build
# Build artifacts are produced by the release pipeline before SRPM generation.
# Keep RPM rebuild deterministic and packaging-only for this scaffold phase.
:

%check
# Packaging-only scaffold phase; functional checks run in CI before artifact publication.
:

%install
mkdir -p %{buildroot}%{_datadir}/gamenative
cp -r gamenative-linux %{buildroot}%{_datadir}/gamenative/

# Desktop shell launcher
install -D -m 0755 packaging/fedora/gamenative-launcher.sh %{buildroot}%{_bindir}/gamenative

# CLI launcher
install -D -m 0755 packaging/fedora/gamenative-cli-launcher.sh %{buildroot}%{_bindir}/gamenative-cli

install -D -m 0644 packaging/fedora/gamenative.desktop %{buildroot}%{_datadir}/applications/gamenative.desktop
install -D -m 0644 packaging/fedora/gamenative.appdata.xml %{buildroot}%{_datadir}/metainfo/gamenative.appdata.xml

%files
%license LICENSE
%doc README.md
%{_bindir}/gamenative
%{_bindir}/gamenative-cli
%{_datadir}/gamenative/
%{_datadir}/applications/gamenative.desktop
%{_datadir}/metainfo/gamenative.appdata.xml

%changelog
* Wed Apr 01 2026 GameNative Team <release@gamenative.app> - 0.2.0-1
- Add interactive CLI (gamenative-cli) with Steam login, 2FA wait, game library,
  download progress, runtime environment configuration, and game launch
- New :cli Gradle submodule under gamenative-linux/cli

* Tue Mar 31 2026 GameNative Team <release@gamenative.app> - 0.1.0-1
- Initial Fedora ARM64 packaging scaffold
