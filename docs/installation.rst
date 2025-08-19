.. _installation:

Installation
============

This guide will help you install Evolver and its dependencies.

Prerequisites
------------
- Java 11 or higher
- Maven 3.6 or higher
- Python 3.8+ (for documentation)

Installation Steps
-----------------

1. Clone the repository:
   .. code-block:: bash

      git clone https://github.com/jMetal/Evolver.git
      cd Evolver

2. Build the project:
   .. code-block:: bash

      mvn clean install

3. (Optional) Install documentation dependencies:
   .. code-block:: bash

      pip install -r requirements-docs.txt

Verification
-----------
To verify the installation, run the following command:

.. code-block:: bash

   mvn test

This should run all the tests and complete successfully.

Troubleshooting
--------------
- If you encounter any build issues, ensure all prerequisites are installed
- Check that your JAVA_HOME environment variable is set correctly
- For Maven issues, try cleaning the local Maven repository:
  .. code-block:: bash

     mvn dependency:purge-local-repository
