from evolver_dashboard.logs import configure_logging, get_logger

configure_logging()

if __name__ == "__main__":
    logger = get_logger()
    logger.info("Starting Evolver...")
