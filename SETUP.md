# Setup Guide (for non-technical users)

This guide walks you through running **autotradebtc** on your own computer, step by step.
No programming knowledge is required — just follow each step in order and copy/paste the
commands exactly as shown.

The app is a Bitcoin "whale" activity dashboard. It watches large traders and large
buy/sell orders on public exchanges and shows them in a web page. It only **reads** public
market data — it does not connect to your money, your exchange account, or place any trades.
You do **not** need any API keys or paid accounts.

---

## What you need to install (one time)

You will install two free programs. Pick the instructions for your operating system.

### 1. Java 17 or newer

This runs the main application.

- **Mac:**
  1. Go to <https://adoptium.net/>
  2. Click the big download button (it auto-detects your Mac).
  3. Open the downloaded `.pkg` file and click through the installer (Continue → Install).

- **Windows:**
  1. Go to <https://adoptium.net/>
  2. Click the big download button.
  3. Open the downloaded `.msi` file and click Next through the installer. Leave all
     options at their defaults.

**Check it worked:** open a terminal (see the box below) and type:

```
java -version
```

You should see a line with a version number like `17`, `21`, or `25`. Any number 17 or
higher is fine.

> **How to open a terminal**
> - **Mac:** press `Cmd + Space`, type `Terminal`, press Enter.
> - **Windows:** press the Start button, type `PowerShell`, press Enter.
> A window with a blinking cursor appears. This is where you paste commands.

### 2. Docker Desktop

This runs the database (MySQL) for you, so you don't have to install and configure a
database by hand.

1. Go to <https://www.docker.com/products/docker-desktop/>
2. Download the version for your operating system and install it (click through the
   installer with default options).
3. **Start Docker Desktop** from your Applications / Start Menu. Wait until the whale icon
   in your menu bar / system tray stops animating and says "Docker Desktop is running".

**Check it worked:** in your terminal, type:

```
docker --version
```

You should see a version number.

---

## Get the project onto your computer

If you were given a **ZIP file** of the project, unzip it somewhere easy to find, like your
Desktop.

If you were given a **link to GitHub**, click the green **Code** button on that page →
**Download ZIP**, then unzip it to your Desktop.

You should now have a folder named `autotradebtc` (it contains files like `pom.xml`,
`mvnw`, and a `frontend` folder).

---

## Point the terminal at the project folder

Every command below must be run from inside the project folder. To get there, type `cd `
(the letters c, d, and a space) in your terminal, then drag the `autotradebtc` folder from
your file explorer onto the terminal window, then press Enter.

It should look something like:

```
cd /Users/yourname/Desktop/autotradebtc
```

To confirm you're in the right place, type `ls` (Mac) or `dir` (Windows) and press Enter —
you should see `pom.xml` in the list.

---

## Step 1 — Start the database

Make sure Docker Desktop is running (whale icon steady), then run:

```
docker compose up -d
```

The first time, this downloads MySQL and can take a couple of minutes. When it finishes you
will see something like `Container autotradebtc-mysql  Started`.

The database now runs quietly in the background. You only need to do this once per computer
restart.

---

## Step 2 — Start the application

**Mac / Linux:**

```
./mvnw spring-boot:run
```

**Windows (PowerShell):**

```
.\mvnw.cmd spring-boot:run
```

The **first run** downloads a lot of components and may take 5–15 minutes depending on your
internet speed. Later runs take under a minute.

You'll know it's ready when the text stops scrolling and you see a line similar to:

```
Started AutotradebtcApplication in 8.5 seconds
```

Leave this terminal window **open** — closing it stops the app.

---

## Step 3 — Open the dashboard

Open your web browser and go to:

<http://localhost:8080>

The dashboard loads. It may take a few minutes of the app running before whale data begins
to appear, because it collects information on a timer.

---

## Stopping everything

- **Stop the app:** click the terminal running it and press `Ctrl + C`.
- **Stop the database:** run `docker compose stop` in the project folder.

Your data is saved and will still be there next time.

## Starting again later

1. Start Docker Desktop.
2. Open a terminal, `cd` into the project folder.
3. `docker compose up -d`
4. `./mvnw spring-boot:run` (or `.\mvnw.cmd spring-boot:run` on Windows)
5. Open <http://localhost:8080>

---

## Troubleshooting

| Problem | Fix |
| --- | --- |
| `java: command not found` or `java` version is below 17 | Java isn't installed correctly. Reinstall from <https://adoptium.net/>, then close and reopen the terminal. |
| `docker: command not found` | Docker Desktop isn't installed, or you need to close/reopen the terminal after installing it. |
| `Cannot connect to the Docker daemon` | Docker Desktop app isn't running. Open it and wait for the whale icon to go steady. |
| App error mentioning `Communications link failure` or `Connection refused` on port 3306 | The database isn't up yet. Run `docker compose up -d` and wait 30 seconds, then start the app again. |
| `Port 8080 ... already in use` | Another program (or a previous copy of this app) is using the port. Close other terminals running the app, or restart your computer. |
| Browser shows "This site can't be reached" at localhost:8080 | The app isn't finished starting, or the terminal running it was closed. Check the app's terminal for the `Started AutotradebtcApplication` line. |
| First `./mvnw` run fails with a download/network error | Check your internet connection and run the same command again — it resumes where it left off. |
| `permission denied: ./mvnw` on Mac | Run `chmod +x mvnw` once, then try again. |

---

## For reference: what's running

- **MySQL database** — in Docker, on port 3306. Username `root`, password `root`
  (see `docker-compose.yml`). The app creates its tables automatically on first start.
- **The application** — a Spring Boot server on port 8080. It serves both the data API and
  the dashboard web page.
- **The dashboard** — a pre-built web page bundled inside the app. You do **not** need
  Node.js or to build the `frontend/` folder unless you are changing the dashboard's code
  (see `CLAUDE.md` for that).
- **External data sources** — public APIs from Blockstream, Hyperliquid, Binance, OKX, and
  Bybit. All free, no accounts or keys. You just need a working internet connection.

If you need to change the database password or connection settings, they live in
`src/main/resources/application.properties`.
