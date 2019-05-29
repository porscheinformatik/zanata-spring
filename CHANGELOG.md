# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
