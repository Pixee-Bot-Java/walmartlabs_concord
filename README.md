# Concord

[![Build Status](https://ci.walmart.com/buildStatus/icon?job=concord)](https://ci.walmart.com/job/concord/)

## Build Dependencies

- [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Docker Community Edition](https://www.docker.com/community-edition)
- Python, pip, and the following python library:
  ```shell
  pip install grpcio-tools
  ```
- [NodeJS and NPM](https://nodejs.org/en/download/) (Node 6 or greater)

## Building

```shell
git clone ...
cd concord
./mvnw clean install -DskipTests
```

### Docker Images

You can build docker images using this commands:

```shell
./mvnw clean install -DskipTests -Pdocker
```

#### Running Docker Images

User Authentication requires an LDAP server.

- Create a LDAP.Properties file as detailed in steps 4 and 5 of the [Open LDAP setup](https://github.com/takari/concord/blob/openldap-setup/docs/openldap/README.md). If you have your own LDAP server setup, you can substitute those details into the file.
- Set the LDAP_CFG environment variable to the location of the `oldap.properties` file you created.

Now the docker containers can be started with the following helper scripts found in the [docker-images](docker-images) directory.

The `run` script will pull docker images from docker hub while `run_local` will start the docker containers you built locally from this project.

On Linux use

- [run.sh](docker-images/run.sh)
- [run_local.sh](docker-images/run_local.sh)

On OSX use these instead

- [run_osx.sh](docker-images/run_osx.sh)
- [run_local_osx.sh](docker-images/run_local_osx.sh)

### Console

Console can be built and ran separately from within the console directory.

```shell
cd ./console
npm install # Install dependencies
```

In order for the console to successfully startup the proxy property in [console/package.json](console/package.json) needs to point to a running concord server instance.

```shell
npm run start # Starts the Concord Console proxied to concord server
```

### Integration tests

#### Prerequisites

Prerequisites:

- Docker, listening on `tcp://127.0.0.1:2375`;
- Ansible 2.4.2+ must be installed and available in `$PATH`.
  See [the official documentation](http://docs.ansible.com/ansible/intro_installation.html);
- Java must be available in `$PATH` as `java`.

#### Running tests

Integration tests are disabled by default. Use the `it` profile to enable them:

```shell
./mvnw verify -Pit
```

This will run ITs agains the locally running server and the agent.
To automatically start and stop the server and the agent using docker, use the
`docker` profile:

```shell
./mvnw verify -Pit -Pdocker
```

## Documentation

See [the website](http://concord.walmart.com).

## Examples

See the [examples](examples) directory.
