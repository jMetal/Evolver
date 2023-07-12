lint:
	@python -m ruff evolver-dashboard/src/

format:
	@python -m black evolver-dashboard/src/
	@python -m isort --profile black evolver-dashboard/src/
