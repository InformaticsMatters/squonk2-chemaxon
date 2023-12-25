# NOTE - this Dockerfile is only for building a custom image that contains the ChemAxon license file so that the
# tests can be run. DO NOT PUSH THE RESULTING IMAGE.

FROM informaticsmatters/squonk2-chemaxon:latest

# First run ./gradlew dockerBuildImage and then run this Dockerfile to create a container that contains the ChemAxon
# License.
# Build this image like this place a valid license file at license.cxl at the root of this repo, then run:
#   docker build -t informaticsmatters/squonk2-chemaxon:license .
# then replace the "tag: latest" bit in molprops.yaml with "tag: license"
#
# Afterwards remember to change the tags back to latest!

# NOTE: THIS IS AN INTERIM MEASURE.

COPY license.cxl /license.cxl
ENV CHEMAXON_LICENSE_URL=/license.cxl
