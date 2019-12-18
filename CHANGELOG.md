# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.8.0] - 2019-12-18

 - Switched to spring-boot-dependencies version 2.2.2 (from platform-bom)
 - Fix filtering by state for allProperties (#14)

## [1.7.0] - 2019-07-25

### Added
 - Accept translations only when they are marked as Translated or Accepted.
  Make it Configurable which states are accepted. (#13)

## [1.6.0] - 2019-07-18

### Added
 - Load translations for variant locales such as de-DE-VARIANT (#9)

### Fixed
 - MessageFormat throws an error when texts contain curly brackets. Fixed by overwriting `resolveCodeWithoutArguments` of `AbstractMessageSource`

## [1.5.0] - 2019-07-16

### Changed
 - Only load languages wich are maintained for the project (#8)

## [1.4.0] - 2019-05-29

### Added
 - Added check for existing locales, to prevent ZanataServiceException: Locale
   {locale} is not allowed for project

## [1.3.0] - 2019-01-25

### Added
 - Added `ZanataAuthenticationInterceptor` to handle Zanata authentication
 - Added `useAuthentcation` to `ZanataMessageSource`
### Changed
 - Use JSON instead of XML for in the REST API
 - Switched to Spring IO Platform Cairo-SR7

## [1.2.0] - 2018-09-25
### Added
 - This Changelog
 - Documentation about authentication for Zanata in README
### Changed
 - Switched to Spring IO Platform Cairo-SR3
### Fixed
 - Zanata translations should be preferred to local translations

## [1.1.0] - 2018-02-13
### Added
 - `AllPropertiesSource` for getting all `MessageSource`'s entries as properties

## [1.0.0] - 2018-01-18
### Added
 - ZanataMessageSource.java
