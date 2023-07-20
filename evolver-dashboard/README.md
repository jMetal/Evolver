# Evolver Dashboard
The Evolver Dashboard is a web-based application built with [Streamlit](https://streamlit.io/) that allows to configure and execute [Evolver](https://github.com/jMetal/Evolver) from an user-friendly web application.

# Pre-requisites
* Python 3.9 and [<3.11](https://github.com/whitphx/streamlit-server-state/issues/187)
* This project requires Evolver's jar to be built. Read the [main README](../README.md) for more information.

# Installation
To install the evolver python package by:

* Cloning and installing the package:
```bash
$ git clone https://github.com/jMetal/Evolver.git
# or
$ git clone git@github.com:jMetal/Evolver.git
# go to the evolver-dashboard folder
$ cd evolver-dashboard
# and install the package
$ pip install .
# or for development
$ pip install -e .
```
* Installing the latest version of the package directly from github with https or git:
```bash
$ pip install "evolver @ git+ssh://git@github.com/jMetal/Evolver.git#subdirectory=evolver-dashboard"
# or
$ pip install "evolver @ git+https://github.com/jMetal/Evolver.git#subdirectory=evolver-dashboard"
```

# Execute the dashboard
The easiest way to execute the dashboard is by running the evolver module:
```bash
$ python -m evolver
```

Additionally, you can check the available options by executing:
```bash
$ python -m evolver --help
```

# Execute the dashboard with Docker
Container image: `ghcr.io/jmetal/evolver-dashboard`

Tags: `latest`

Usage:

For basic usage, you don't need to mount any volume, to save the results use the download option in the dashboard
```bash
$ docker run --rm -p 8501:8501 \
ghcr.io/jmetal/evolver-dashboard:latest
```

For more advanced usage, you can mount a volume to save the results in your local machine as well as launch it as a permanent container that you can connect to later
```bash
$ docker run -v <local/output/folder>:/tmp/evolver \
-p 8501:8501 ghcr.io/jmetal/evolver-dashboard:latest
```