- if you have gone through another onboarding guide, some steps can be simplified. watch out for items that start with an exclamation mark (!) though.
- most environment variables here are used only for the guide process convenience, they are not needed later when IDEA is set up. Exceptions will be marked explicitly. Theoretically you can skip most of those unexceptional envvars if you're ready to type the full texts instead.
- `export BACKEND=~/PROJECTS/r8n` adjust to your setup
- if you use a different campus machine, you'd have to reinstall IDEA, but settings for everything else should be transferred already. Check the exclamation mark items just in case.

# Java Development Kit
Execution engine for backend. Used every time to build and run the backend.
- `java --version`, `javac --version` in terminal should show same version 21. If you have a more modern version, it may work depending on whether you manage to force it to use language standard 21 while compiling. So if you see a more modern version, you can try skipping this section and going right to running section. If any problems along the lines of "JDK toolchain 21 not found" arise, you'd have to set up existing JDK or install an older one.
- during extension installation you get a suggestion to install a new JDK, or you can open this window later by selecting 'install new JDK' in the action palette (Ctrl-Shift-P)
- download an archive for JDK 21 (https://github.com/adoptium/temurin21-binaries/releases/)
- campus:
  - `mkdir -p /sgoinfre/goinfre/Perso/$USER/jdk && tar -xzf ~/Downloads/OpenJDK21U-jdk_x64_linux_hotspot_21.0.10_7.tar.gz -C /sgoinfre/goinfre/Perso/$USER/jdk` (using this in examples below)
  - `export JAVA_HOME=/sgoinfre/goinfre/Perso/$USER/jdk/jdk21.0.10+7`
- personal machine if you're ready for a system-wide installation:
  - `mkdir -p /opt/jdk && tar -xzf ~/Downloads/OpenJDK21U-jdk_x64_linux_hotspot_21.0.10_7.tar.gz -C /opt/jdk`
  - `export JAVA_HOME=/opt/jdk/jdk21.0.10+7`
- personal machine with a single-user installation:
  - `mkdir -p ~/jdk && tar -xzf ~/Downloads/OpenJDK21U-jdk_x64_linux_hotspot_21.0.10_7.tar.gz -C ~/jdk`
  - `export JAVA_HOME=~/jdk/jdk21.0.10+7`
- (below: select the configuration file for your preferred terminal that you use in IDEA and independently; important if you want to run Gradle commands related to building the project from it; IDEA uses sh by default; pay attention to different quotes)
- `echo "export JAVA_HOME=$JAVA_HOME" >> ~/.bashrc` (zsh on the campus is bugged; sh and bash definitely work in IDEA, the rest is up to you to set up) (this export is not global, it works only for your selected terminal, but this envvar is unskippable, it tells the terminal where to look for java)
- `echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc` (same as above)
- (reloading Gradle and terminal to make sure all changes are picked up)
- `cd $BACKEND && ./gradlew --stop`
- `pkill -f GradleDaemon`
- close all terminal instances and open a new one
- check `java --version` and `javac --version` again
- `export BACKEND=~/PROJECTS/r8n`
- `cd $BACKEND && ./gradlew javaToolchains`: should display your JDK and 'is JDK: true'

# Gradle
Build and dependency management system for backend. Runs every time you build backend, also probably you'd call Gradle wrapper (gradlew) to run the backend too. Included into the repository, but its cache (downloaded libraries) is stored separately. If you're setting up a personal machine, skip to the next section. If you're setting up a campus machine, you'll have to move Gradle cache to sgoinfre, it's too big for your allowed home folder volume.
- `mkdir -p /sgoinfre/goinfre/Perso/$USER/gradle`
- `rm -rf ~/.gradle`
- `ln -s /sgoinfre/goinfre/Perso/$USER/gradle ~/.gradle` (now ~/.gradle is not a Gradle cache folder, rather a link to an actual cache folder in sgoinfre)
- `echo "export GRADLE_USER_HOME=/sgoinfre/goinfre/Perso/$USER/gradle" >> ~/.bashrc` (or your terminal config) (this export is not global, it works only for your selected terminal, but this envvar is unskippable, it tells your Gradle where its cache actually is in case it decides to not follow the symlink ~/.gradle)
- (reloading Gradle and terminal to make sure all changes are picked up)
- `cd $BACKEND && ./gradlew --stop`
- `pkill -f GradleDaemon`
- close all terminal instances and open a new one
- ! IDEA: File - Settings - Build, Execution, Deployment - Build Tools - Gradle - Gradle user home: /sgoinfre/goinfre/Perso/$USER/gradle (substitute by yourself). Don't open the navigation window, just type the address right in the main settings window.

# Editing backend
- IDEA - File - Open - select $BACKEND
- (backend.code-workspace is a VSCode-only file)

# Running backend in IDEA
- might not work on campus, all terminals can bug out being run in IDEA. addressed in tickets #21, #22
- open Gradle panel on the right 
  - if it's not there, open $BACKEND/settings.gradle.kts, (top right corner) Link the Gradle project
- for each service in the order of their interdependency (gateway last):
  - find its bootRun task, right-click, Modify run configuration
  - open Environment variables field, insert everything from local.env there
  - call that modified bootRun

# Running backend in an external terminal
- only gateway and opinions-sv is runnable currently
- `make docker-run-database`
- `make local-run-opinions`, see opinions.log filled and opinions-sv started successfully
- (manual access to database) `make docker-database-connect`, `\c r8n` (connect to database), `set schema 'opinions';`, `\dt` to see five tables, `select * from opinions;` to see some data
- `make build-opinions` - see build succeeding
- `make local-stop-all`
- `make local-run-all` and check all the log files for Started application in X seconds
- `make direct-request-opinion` and `make routed-request-opinion` should provide same result, with 0000..0 id

# Docker run via Makefile (HTTPS)
- service list:
  - `deployment/config/services.list`
  - when adding a new service, add its name there so Makefile targets and TLS certificate generation include it
- create local secrets file (do once and do not commit it):
  - `cp deployment/secrets/docker.secrets.env.example deployment/secrets/docker.secrets.env`
  - set values in `deployment/secrets/docker.secrets.env`:
    - `TLS_KEYSTORE_PASSWORD=...`
    - `TLS_TRUSTSTORE_PASSWORD=...`
- TLS certificates are generated automatically by `make docker-up` (`docker-certs` target), stored in `deployment/certs/internal` (services) and `deployment/certs/edge` (Nginx), and reused if already present and valid.
- start Docker services:
  - `make docker-up`
- watch logs:
  - `make docker-logs`
  - or per service: `make docker-logs-gateway`, `make docker-logs-opinions`, `make docker-logs-mock`
- test HTTPS routed requests through gateway:
  - `make routed-request-opinion ENV=docker`
  - `make routed-request-mock END=docker`
- stop Docker services (required!):
  - `make docker-down`

# Frontend setup
- open a new terminal (backend can keep running in another one)
- `export FRONTEND=~/PROJECTS/r8n/frontend` adjust to your setup
- check if Node.js is installed:
  - `node -v`
  - if command is not found, install Node via nvm:
    - `curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash`
    - restart terminal
- use the project's Node version:
  - `cd $FRONTEND`
- `nvm install`
- `nvm use`
- verify correct Node version:
  - `node -v` should be `>= 22.13.0`
  - `npm -v` should be `10+`
- if you run frontend on a campus machine, move npm cache to sgoinfre:
  - `npm config set cache /sgoinfre/goinfre/Perso/$USER/.npm-cache --global`
  - `npm config get cache` should print `/sgoinfre/goinfre/Perso/$USER/.npm-cache`
- install dependencies:
  - `npm ci`
- start frontend dev server:
  - `npm run dev` for standalone UI work
  - `make frontend-dev` if you need frontend proxying to local backend over HTTPS with project certificate
  - as soon as you see `Local: http://127.0.0.1:5173/` in the logs, the app is running
- open `http://127.0.0.1:5173` in browser
- for API integration keep backend services running in another terminal: `make local-run-all`

# Frontend commands for daily work
- `cd ~/PROJECTS/r8n/frontend`
- `nvm use`
- `npm run dev` to run UI locally
- `make frontend-dev` to run UI with gateway certificate when proxying `/api` to local backend
- `npm run lint` to run ESLint
- `npm test` to run Vitest tests
- `npm run build` to verify production build

# Frontend after repository update
- stop dev server in running terminal (`Ctrl-C` or `q`)
- `cd ~/PROJECTS/r8n && git pull`
- `cd ~/PROJECTS/r8n/frontend`
- `nvm use`
- `npm ci` to sync dependencies with updated lockfile
- `npm run lint` and `npm test`
- `npm run dev` and verify `http://127.0.0.1:5173`

# IDEA troubleshooting
- `com.intellij.platform.ide.bootstrap.DirectoryLock$CannotActivateException: Process "/app/bin/idea" (2) is still running and does not respond.`
  - reason: if you were automatically logged out in the campus, IDEA didn't shut down properly
  - solution: `rm ~/.var/app/com.jetbrains.IntelliJ-IDEA-Community/config/JetBrains/IdeaIC2025.3/.lock`
- not enough space
  - clean up as much as possible. delete all unused projects, evals, downloads, clear browser cache, clear trash. Home folder is only 5G, this is to be expected. Alternatively you can try downloading IDEA separately and installing it to sgoinfre.
  - there is a magic rule in the makefile, but it can be dangerous
