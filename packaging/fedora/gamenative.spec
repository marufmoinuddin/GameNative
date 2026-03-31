%global app_version 0.1.0

Name:           gamenative
Version:        %{app_version}
Release:        1%{?dist}
Summary:        GameNative Linux launcher for ARM64

License:        Apache-2.0
URL:            https://github.com/maruf/GameNative
Source0:        %{name}-%{version}-aarch64.tar.gz

BuildArch:      aarch64

Requires:       wine
Requires:       box64
Requires:       mesa-vulkan-drivers
Requires:       libsecret
Requires:       java-17-openjdk-headless

%description
GameNative desktop launcher and runtime orchestration stack for Linux ARM64.

%prep
%setup -q -c -T
tar -xzf %{SOURCE0}

%build
./gradlew -p gamenative-linux :core:runtime:compileKotlin :desktop:shell:compileKotlin

%install
mkdir -p %{buildroot}%{_datadir}/gamenative
cp -r gamenative-linux %{buildroot}%{_datadir}/gamenative/
install -D -m 0644 packaging/fedora/gamenative.desktop %{buildroot}%{_datadir}/applications/gamenative.desktop
install -D -m 0644 packaging/fedora/gamenative.appdata.xml %{buildroot}%{_datadir}/metainfo/gamenative.appdata.xml

%files
%license LICENSE
%doc README.md
%{_datadir}/gamenative/
%{_datadir}/applications/gamenative.desktop
%{_datadir}/metainfo/gamenative.appdata.xml

%changelog
* Tue Mar 31 2026 GameNative Team <release@gamenative.app> - 0.1.0-1
- Initial Fedora ARM64 packaging scaffold
