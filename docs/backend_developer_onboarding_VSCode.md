- if you have gone through another onboarding guide, some steps can be simplified. watch out for items that start with an exclamation mark (!) though.
- most environment variables here are used only for the guide process convenience, they are not needed later when VSCode is set up. Exceptions will be marked explicitly. Theoretically you can skip most of those unexceptional envvars if you're ready to type the full texts instead.
- `export BACKEND=~/PROJECTS/r8n` adjust to your setup

# VSCode extensions
- Kotlin Language by mathiasflohlich
- Extension Pack for Java by Microsoft

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
- ! VSCode: 'add Java runtime' in action palette (Ctrl-Shift-P), select $JAVA_HOME
- (below: select the configuration file for your preferred terminal that you use in VSCode and independently; important if you want to run Gradle commands related to building the project from it; pay attention to different quotes)
- `echo "export JAVA_HOME=$JAVA_HOME" >> ~/.zshrc` (.bashrc etc.)
- `echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc`
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
- `ln -s /sgoinfre/goinfre/Perso/$USER/gradle ~/.gradle`
- `echo "export GRADLE_USER_HOME=/sgoinfre/goinfre/Perso/$USER/gradle" >> ~/.zshrc` (or your terminal config) (this export is not global, it works only for your selected terminal, but this envvar is unskippable, it tells your Gradle where its cache actually is in case it decides to not follow the symlink ~/.gradle)
- (reloading Gradle and terminal to make sure all changes are picked up)
- `cd $BACKEND && ./gradlew --stop`
- `pkill -f GradleDaemon`
- close all terminal instances and open a new one

# Editing backend
  - open backend.code-workspace
  - agree to use repository from parent level
  - edit code

# Running backend in VSCode
- open backend.code-workspace
- action palette, 'Run task'
- select 'bootRun gateway' on top
- VSCode terminal opens automatically
- as soon as you see 'Started GatewayApplicationKt in X seconds' in the logs in the terminal - the app is running
- return to running terminal, Ctrl-C and press any key to terminate

# Running backend in an external terminal
- all 5 services are runnable: gateway, opinions, mock, users, export
- `make docker-run-database`
- `make local-run-opinions`, see opinions.log filled and opinions-sv started successfully
- `make direct-request-opinion`, see valid result
- can play around with the request from previous command to get failures
- (manual access to database) `make docker-database-connect`, `\c r8n` (connect to database), `set schema 'opinions';`, `\dt` to see five tables, `select * from opinions;` to see some data
- `make build-opinions` - see build succeeding
- `make local-stop-all`
- `make local-run-all` and check all the log files for Started application in X seconds
- `make direct-request-opinion` and `make routed-request-opinion` should provide same result, with 0000..0 id

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
- stop frontend dev server in running terminal (`Ctrl-C` or `q`)
- `cd ~/PROJECTS/r8n && git pull`
- `cd ~/PROJECTS/r8n/frontend`
- `nvm use`
- `npm ci` to sync dependencies with updated lockfile
- `npm run lint` and `npm test`
- `npm run dev` and verify `http://127.0.0.1:5173`

for expanded development experience (better code navigation, code suggestions) try IntelliJ IDEA onboarding guide
