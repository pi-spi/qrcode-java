# io.github.pi-spi:bceao-pispi-qrcode

SDK Java officiel pour les QR Codes PI-SPI (BCEAO)
Générez et validez facilement les payloads EMV conformes à la norme EMV.
Ce SDK facilite l’intégration des applications Java avec l’écosystème PI-SPI de la BCEAO.
Utilisez ensuite la bibliothèque de génération de QR Code de votre choix pour afficher le QR code.

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.pi-spi</groupId>
    <artifactId>bceao-pispi-qrcode</artifactId>
    <version>1.0.2</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.pi-spi:bceao-pispi-qrcode:1.0.2'
```

Ou en Kotlin DSL :

```kotlin
implementation("io.github.pi-spi:bceao-pispi-qrcode:1.0.2")
```

### Depuis les sources (répertoire local Maven)

À la racine du dépôt, avec JDK 21 et Maven 3.9+ :

```bash
mvn clean install
```

Cela installe `io.github.pi-spi:bceao-pispi-qrcode:1.0.2` dans `~/.m2/repository`. En cas d’erreur du type « Failed to install artifact » ou « No such file or directory » vers `.m2`, vérifiez les droits d’écriture sur le répertoire `~/.m2/repository`.

## Usage rapide

```java
import bceao.pispi.qrcode.*;

// Générer une payload
String payload = PispiQrCode.buildPayloadString(
    QrPayloadInput.builder()
        .alias("3497a720-ab11-4973-9619-534e04f263a1")
        .countryCode("CI")
        .qrType(QrType.STATIC)
        .referenceLabel("CAISSE_A01")
        .amount(1500L)
        .build()
);
// Utiliser `payload` avec une librairie QR pour générer l'image
```

## Exemples complets

Des exemples de projets Maven et Gradle prêts à l'emploi sont disponibles dans le dépôt [qrcode-samples/java](https://github.com/pi-spi/qrcode-samples/tree/main/java).

## API


| Méthode                                                            | Description                                           |
| ------------------------------------------------------------------ | ----------------------------------------------------- |
| `PispiQrCode.createQrPayload(QrPayloadInput)`                      | Génère la payload EMV (résultat avec la chaîne).      |
| `PispiQrCode.createQrPayload(QrPayloadInput, QrPayloadOptions)`    | Idem avec options (données additionnelles).           |
| `PispiQrCode.buildPayloadString(QrPayloadInput)`                   | Raccourci qui retourne directement la chaîne payload. |
| `PispiQrCode.buildPayloadString(QrPayloadInput, QrPayloadOptions)` | Idem avec options.                                    |
| `PispiQrCode.isValidPispiQrPayload(String)`                        | Valide une payload et extrait les données.            |
| `CrcUtil.computeCrc16(String)`                                     | Calcule le CRC-16 (CCITT) d'une chaîne.               |


## Types principaux

### `QrPayloadInput`

Paramètres d'entrée pour générer une payload EMV. Construit via `QrPayloadInput.builder()`.


| Paramètre        | Type     | Description                                      |
| ---------------- | -------- | ------------------------------------------------ |
| `alias`          | `String` | UUID v4, identifiant du compte marchand          |
| `countryCode`    | `String` | Code ISO2 UEMOA (BJ, BF, CI, ML, NE, SN, TG, GW) |
| `qrType`         | `QrType` | Type de QR : STATIC ou DYNAMIC                   |
| `referenceLabel` | `String` | Libellé de référence, max 25 caractères          |
| `amount`         | `String` | Montant optionnel ; chiffres uniquement, max 13  |


### `QrPayloadOptions`

Options pour la génération (surcharge des données additionnelles). Construit via `QrPayloadOptions.builder()`.


| Paramètre        | Type                      | Description                         |
| ---------------- | ------------------------- | ----------------------------------- |
| `additionalData` | `AdditionalDataOverrides` | Données additionnelles (tag 62 EMV) |


### `AdditionalDataOverrides`

Surcharges des données additionnelles (template 62). Construit via `AdditionalDataOverrides.builder()`.


| Paramètre         | Type                 | Description                                                                            |
| ----------------- | -------------------- | -------------------------------------------------------------------------------------- |
| `merchantChannel` | `String`             | Canal marchand (tag 11)                                                                |
| `custom`          | `Map<String,String>` | Données libres ; clés autorisées : `05` (max 25 car.) et `11` (valeur `000` ou `400`). |


### `QrPayloadResult`

Résultat de la génération. Record avec `payload()`.


| Paramètre | Type     | Description                       |
| --------- | -------- | --------------------------------- |
| `payload` | `String` | Chaîne payload EMV complète (CRC) |


### `QrValidationResult`

Résultat de la validation. Record avec `valid()`, `errors()`, `data()`.


| Paramètre | Type             | Description                                |
| --------- | ---------------- | ------------------------------------------ |
| `valid`   | `boolean`        | `true` si la payload respecte la structure |
| `errors`  | `List<String>`   | Liste des erreurs (vide si valide)         |
| `data`    | `QrPayloadInput` | Données extraites si valide, sinon `null`  |


### `QrType`

Enum des types de QR Code.


| Valeur    | Description                                |
| --------- | ------------------------------------------ |
| `STATIC`  | QR avec montant fixe                       |
| `DYNAMIC` | QR sans montant (saisie par l'utilisateur) |


## Validation d'une payload

```java
QrValidationResult result = PispiQrCode.isValidPispiQrPayload(payload);

if (result.valid()) {
    QrPayloadInput data = result.data();
    // alias, countryCode, qrType, referenceLabel, amount
} else {
    for (String error : result.errors()) {
        System.err.println(error);
    }
}
```

## Données additionnelles (options)

```java
AdditionalDataOverrides overrides = AdditionalDataOverrides.builder()
    .putCustom("05", "REFERENCE_TX")   // max 25 caractères
    .putCustom("11", "400")            // "000" ou "400"
    .build();
QrPayloadOptions options = QrPayloadOptions.builder()
    .additionalData(overrides)
    .build();

QrPayloadResult result = PispiQrCode.createQrPayload(input, options);
```

## Contraintes


| Champ            | Type     | Description                                          |
| ---------------- | -------- | ---------------------------------------------------- |
| `alias`          | `String` | UUID v4 **obligatoire**                              |
| `countryCode`    | `String` | Codes ISO2 UEMOA : BJ, BF, CI, ML, NE, SN, TG, GW    |
| `referenceLabel` | `String` | **Obligatoire**, max 25 caractères                   |
| `amount`         | `Long`   | Optionnel ; si présent : chiffres uniquement, max 13 |


## Structure du projet

Le projet est organisé en un module Maven unique (`qrcode`) contenant le SDK. Le POM parent (`bceao-pispi-qrcode-parent`) gère la version Java et la configuration commune.

```
qrcode-java/                    # Racine du projet
├── pom.xml                     # POM parent
├── qrcode/                     # Module SDK
│   ├── pom.xml                 # Configuration du module
│   └── src/
│       ├── main/java/.../      # Classes principales
│       │   ├── AdditionalDataOverrides.java   # Données additionnelles personnalisables
│       │   ├── CrcUtil.java                  # Calcul CRC-16 (CCITT)
│       │   ├── EmvCodec.java                 # Encodage/décodage EMV
│       │   ├── PispiQrCode.java              # Classe principale, façade API
│       │   ├── QrPayloadInput.java           # Entrée pour génération payload
│       │   ├── QrPayloadOptions.java         # Options (données additionnelles)
│       │   ├── QrPayloadResult.java          # Résultat à parti payload
│       │   ├── QrType.java                   # Enum STATIC / DYNAMIC
│       │   └── QrValidationResult.java       # Résultat de validation
│       └── test/java/.../      # Tests unitaires
│           └── PispiQrCodeTest.java          # Tests unitaires du SDK
├── README.md                   # Documentation
└── DEPLOYMENT.md               # Guide de déploiement Maven Central
```

## Développement

```bash
# Compiler
mvn compile

# Tester
mvn test

# Installer localement
mvn install
```

## Licence

MIT.