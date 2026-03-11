# Guide de déploiement - int.bceao.pispi:qrcode

Ce guide explique comment publier l’artifact Maven `int.bceao.pispi:qrcode` vers un dépôt Maven (Sonatype OSSRH / Maven Central).

## Prérequis

1. **Compte Sonatype** : Créer un compte sur [central.sonatype.com](https://central.sonatype.com/) et configurer le projet (groupId `int.bceao.pispi` doit être associé à votre compte / namespace).

2. **GPG** : Clé GPG pour signer les artifacts (génération et publication sur un serveur de clés).

3. **Paramètres Maven** : Dans `~/.m2/settings.xml`, configurer le serveur `ossrh` avec les identifiants Sonatype (token ou login/mot de passe).

Exemple `settings.xml` :

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>VOTRE_USERNAME_SONATYPE</username>
      <password>VOTRE_TOKEN_OU_MOT_DE_PASSE</password>
    </server>
  </servers>
</settings>
```

## Étapes de déploiement

### 1. Préparer le build

```bash
# Compiler et lancer les tests (SDK uniquement)
mvn -pl qrcode clean verify

# Ou tout le projet
mvn clean verify
```

### 2. Vérifier la version

Dans `qrcode/pom.xml` (ou le parent `pom.xml`), vérifier que la version est correcte (ex. `0.1.2`). Suivre [Semantic Versioning](https://semver.org/) pour les montées de version.

### 3. Signer et déployer vers OSSRH (staging)

Le déploiement vers Sonatype OSSRH se fait en général avec :

- **maven-gpg-plugin** : signer les artifacts (jar, sources, javadoc).
- **nexus-staging-maven-plugin** : envoyer vers le dépôt de staging et fermer / libérer le staging.

Exemple de plugins à ajouter dans `qrcode/pom.xml` (à adapter selon votre processus) :

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-gpg-plugin</artifactId>
  <version>3.2.1</version>
  <executions>
    <execution>
      <id>sign-artifacts</id>
      <phase>verify</phase>
      <goals>
        <goal>sign</goal>
      </goals>
    </execution>
  </executions>
</plugin>
<plugin>
  <groupId>org.sonatype.plugins</groupId>
  <artifactId>nexus-staging-maven-plugin</artifactId>
  <version>1.6.13</version>
  <extensions>true</extensions>
  <configuration>
    <serverId>ossrh</serverId>
    <nexusUrl>https://s01.oss.sonatype.org/</nexusUrl>
    <autoReleaseAfterClose>true</autoReleaseAfterClose>
  </configuration>
</plugin>
```

Puis, depuis la racine (ne déploie que le module SDK, pas le sample) :

```bash
mvn -pl qrcode clean deploy
```

Ou depuis le module SDK :

```bash
cd qrcode && mvn clean deploy
```

Cela envoie uniquement l’artifact `int.bceao.pispi:qrcode` (signé) vers le dépôt de staging OSSRH. Le module `qrcode-sample` n’est pas publié. Si `autoReleaseAfterClose` est à `true`, le staging est fermé et libéré automatiquement ; sinon, il faut fermer et libérer manuellement depuis l’interface Sonatype.

### 4. Vérifier la publication

- **Staging** : [https://s01.oss.sonatype.org](https://s01.oss.sonatype.org) → voir le repository de staging et l’activité de déploiement.
- **Maven Central** : après propagation (souvent 10–30 minutes), vérifier sur [search.maven.org](https://search.maven.org/) avec `int.bceao.pispi` et `qrcode`.

### 5. Dépendance côté utilisateur

Une fois publié, les utilisateurs pourront déclarer :

```xml
<dependency>
  <groupId>int.bceao.pispi</groupId>
  <artifactId>qrcode</artifactId>
  <version>0.3.2</version>
</dependency>
```

## Mise à jour d’une version

1. Modifier la version dans le parent `pom.xml` (et éventuellement `qrcode/pom.xml`).
2. Exécuter `mvn -pl qrcode clean verify`.
3. Exécuter `mvn -pl qrcode deploy` (avec GPG et OSSRH configurés).

## Notes importantes

1. **groupId** : Le groupe `int.bceao.pispi` doit être réservé / configuré dans votre compte Sonatype (namespace).
2. **Signature** : Maven Central exige des artifacts signés (GPG). La clé publique doit être publiée sur un serveur de clés (ex. keyserver.ubuntu.com).
3. **Javadoc / sources** : Les plugins `maven-javadoc-plugin` et `maven-source-plugin` sont déjà configurés dans `qrcode/pom.xml` pour générer les artifacts annexes.
4. **Sécurité** : Ne pas committer de tokens ou mots de passe ; utiliser `settings.xml` (hors dépôt) ou un outil de gestion des secrets.
