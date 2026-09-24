.. _installation:

Installation
============

This guide will help you install Evolver and its dependencies.

Prerequisites
-------------
- Java 21 or higher (JDK 21 recommended)
- Maven 3.6 or higher
- Python 3.8+ (for documentation)

JDK 21, an LTS release, is the version used by the continuous integration workflows. Newer JDKs can
compile the project, but some build plugins may not support them yet (for instance, SpotBugs, which
runs during ``mvn verify``, fails with JDK 26). If several JDKs are installed, make ``JAVA_HOME``
point to JDK 21; ``mvn -v`` shows the one Maven is using.

Installation Steps
------------------

1. Clone the repository:
   .. code-block:: bash

      git clone https://github.com/jMetal/Evolver.git
      cd Evolver

2. Build the project:
   .. code-block:: bash

      mvn clean install

3. (Optional) Install documentation dependencies:
   .. code-block:: bash

      pip install -r docs/requirements-docs.txt

Verification
------------
To verify the installation, run the following command:

.. code-block:: bash

   mvn test

This should run all the tests and complete successfully.

Troubleshooting
---------------
- If you encounter any build issues, ensure all prerequisites are installed
- Check that your JAVA_HOME environment variable points to a JDK 21 or higher (``mvn -v`` shows the Java version Maven uses)
- For Maven issues, try cleaning the local Maven repository:
  .. code-block:: bash

     mvn dependency:purge-local-repository
