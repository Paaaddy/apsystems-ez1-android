# Changelog

## [1.3.1](https://github.com/Paaaddy/apsystems-ez1-android/compare/v1.3.0...v1.3.1) (2026-06-05)


### Bug Fixes

* bump gradle dependencies to latest ([fe79f6a](https://github.com/Paaaddy/apsystems-ez1-android/commit/fe79f6a392eef7616f5481478ba4c0c15457d649))

## [1.3.0](https://github.com/Paaaddy/apsystems-ez1-android/compare/v1.2.1...v1.3.0) (2026-05-15)


### Features

* smart notifications, device fingerprint binding, stale data UX, security hardening ([#20](https://github.com/Paaaddy/apsystems-ez1-android/issues/20)) ([794af38](https://github.com/Paaaddy/apsystems-ez1-android/commit/794af38d0a735d3f042f53413dc437ddcf052c8f))

## [1.2.1](https://github.com/Paaaddy/apsystems-ez1-android/compare/v1.2.0...v1.2.1) (2026-05-07)


### Performance Improvements

* **ci:** cap build at 30min, fix Release Please APK trigger ([32fe13e](https://github.com/Paaaddy/apsystems-ez1-android/commit/32fe13e6b7ed9c485a6a31a5dfab411ab5babbea))
* **ci:** skip build for docs commits; always attach debug APK to releases ([d781a6c](https://github.com/Paaaddy/apsystems-ez1-android/commit/d781a6ccfa59f45bda90fe59539106690ae8356e))

## [1.2.0](https://github.com/Paaaddy/apsystems-ez1-android/compare/v1.1.0...v1.2.0) (2026-05-06)


### Features

* add GitHub Actions CI/CD, Dependabot, and README ([537e43f](https://github.com/Paaaddy/apsystems-ez1-android/commit/537e43f382689b005216887394b9cfaaba8abcea))
* APsystems EZ1 Android monitor app ([7c33316](https://github.com/Paaaddy/apsystems-ez1-android/commit/7c33316bba266d969cb32a62a482d5e98916c619))
* demo mode, debug log export, EZ1DataSource interface ([66e2d9d](https://github.com/Paaaddy/apsystems-ez1-android/commit/66e2d9dee3d11bace8ae13da70ac5c38a2297dbf))
* update mechanism — versionCode, Obtainium support, faster CI ([4b3b143](https://github.com/Paaaddy/apsystems-ez1-android/commit/4b3b143f105b7a2ec4c7973c40ff3b52c63f6e47))


### Bug Fixes

* **ci:** align release tag format and fix release workflow trigger ([e9da94d](https://github.com/Paaaddy/apsystems-ez1-android/commit/e9da94da7472db36d846ada3b7b40211b51bc822))
* **ci:** extract semver from component-prefixed release tags ([e7055d2](https://github.com/Paaaddy/apsystems-ez1-android/commit/e7055d2e0b2ef4253da2e1ef89669cfeee39fd13))
* make FakeEZ1DataSource open, remove onCleared test (protected method) ([35acc1d](https://github.com/Paaaddy/apsystems-ez1-android/commit/35acc1dfb73713d391ea7154e7b1818192cb0c10))
* move DataSourceCreator to src/debug and src/release source sets ([8418b2c](https://github.com/Paaaddy/apsystems-ez1-android/commit/8418b2cb9a885a7568acb4614d8b213c8161a8ea))
* move DemoDataSourceTest to debugUnitTest source set ([8b5af14](https://github.com/Paaaddy/apsystems-ez1-android/commit/8b5af148a73224ed842b57fc4999e16c064286bd))


### Performance Improvements

* **ci:** speed up test & lint step ([d68c399](https://github.com/Paaaddy/apsystems-ez1-android/commit/d68c399492b3758e2dcd6d3b7f15dd00c78babd6))

## [1.1.0](https://github.com/Paaaddy/apsystems-ez1-android/compare/ez1-monitor-v1.0.0...ez1-monitor-v1.1.0) (2026-05-06)


### Features

* add GitHub Actions CI/CD, Dependabot, and README ([537e43f](https://github.com/Paaaddy/apsystems-ez1-android/commit/537e43f382689b005216887394b9cfaaba8abcea))
* APsystems EZ1 Android monitor app ([7c33316](https://github.com/Paaaddy/apsystems-ez1-android/commit/7c33316bba266d969cb32a62a482d5e98916c619))
* demo mode, debug log export, EZ1DataSource interface ([66e2d9d](https://github.com/Paaaddy/apsystems-ez1-android/commit/66e2d9dee3d11bace8ae13da70ac5c38a2297dbf))
* update mechanism — versionCode, Obtainium support, faster CI ([4b3b143](https://github.com/Paaaddy/apsystems-ez1-android/commit/4b3b143f105b7a2ec4c7973c40ff3b52c63f6e47))


### Bug Fixes

* make FakeEZ1DataSource open, remove onCleared test (protected method) ([35acc1d](https://github.com/Paaaddy/apsystems-ez1-android/commit/35acc1dfb73713d391ea7154e7b1818192cb0c10))
* move DataSourceCreator to src/debug and src/release source sets ([8418b2c](https://github.com/Paaaddy/apsystems-ez1-android/commit/8418b2cb9a885a7568acb4614d8b213c8161a8ea))
* move DemoDataSourceTest to debugUnitTest source set ([8b5af14](https://github.com/Paaaddy/apsystems-ez1-android/commit/8b5af148a73224ed842b57fc4999e16c064286bd))


### Performance Improvements

* **ci:** speed up test & lint step ([d68c399](https://github.com/Paaaddy/apsystems-ez1-android/commit/d68c399492b3758e2dcd6d3b7f15dd00c78babd6))
