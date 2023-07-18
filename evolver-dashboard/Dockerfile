FROM evolver:latest

# Install python3 and pip
RUN apt update && apt install -y python3 python3-pip

WORKDIR /evolver

# Install python dependencies
COPY pyproject.toml .
COPY README.md .
COPY src ./src


# Install python dependencies
RUN python3 -m pip install "."

ENTRYPOINT []
CMD ["python3", "-m", "evolver", "--jar", "Evolver-1.0-SNAPSHOT-jar-with-dependencies.jar"]