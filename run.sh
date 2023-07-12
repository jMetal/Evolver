#!/bin/bash

create_venv() {
  local venv_name="$1"

  # Check if the virtual environment already exists
  if [ ! -d "$venv_name" ]; then
    python3 -m venv "$venv_name"
    echo "Virtual environment created."
  else
    echo "Python virtual environment already exists:"
  fi
}

activate_venv() {
  local venv_name="$1"

  if [ ! -d "$venv_name" ]; then
    echo "Python virtual environment does not exist:, Creating..."
    create_venv "$VENV_NAME"
  fi

  source "$venv_name/bin/activate"
  echo "Virtual environment activated."
}

setup_enviroment() {
  local venv_name="$1"

  if [ -d "$venv_name" ]; then
    echo "Activating virtual environment"
    activate_venv "$venv_name"
  else
    activate_venv "$venv_name"
    echo "Installing evolver and dependencies..."
    cd evolver-dashboard
    pip install -e "."
    cd ..
    echo "Dependencies installed."
  fi
}

build_evolver() {
  if [ ! -d "target/Evolver-1.0-SNAPSHOT-jar-with-dependencies.jar" ]; then
    echo "Building evolver"

    if ! command -v mvn &> /dev/null ; then
      echo "Maven is not installed, please install maven to build Evolver."
      exit -1
    fi

    mvn package
  fi
}

# Build Evolver if it does not exist
build_evolver

# Virtual environment name as command-line argument
VENV_NAME="$PWD/.venv-evolver"

# Call the function to install dependencies
setup_enviroment "$VENV_NAME"

# Run the Python script
python -m evolver
