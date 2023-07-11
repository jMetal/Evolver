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
    echo "Installing evolver-dashboard and dependencies..."
    pip install -e "."
    echo "Dependencies installed."
  fi
}

# Virtual environment name as command-line argument
VENV_NAME="$PWD/.venv-evolver"

# Go to Python's package directory
cd evolver-dashboard

# Call the function to install dependencies
setup_enviroment "$VENV_NAME"

# Run the Python script
python -m evolver_dashboard
