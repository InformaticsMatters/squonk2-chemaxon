# squonk2-chemaxon

ChemAxon jobs repository contains Java code running molecular property predictions.
- **chemaxon-molecular-props-simple** - calculation of various simple molecular properties
- **chemaxon-molecular-props-logd** - logD calculator
- **chemaxon-molecular-props-pka** - pKa calculator
- **chemaxon-gupta-bbb** - Gupta et al. blood brain barrier calculation
- **chemaxon-abbvie-mps** - Abbvie multi parametric drug likeness score
- **chemaxon-pfizer-cns-mpo** - Pfizer CNS MPO score
- **chemaxon-kids-mpo** - multi parametric optimisation score for kinase drugs

NOTE: these jobs require a ChemAxon license file. Get in touch with us if you don't have one.

To run these in Squonk Data Manager you need to add your license file as an asset to
your project, unit or organisation.
See [here](https://informaticsmatters.gitlab.io/squonk2-account-server/2-0/assets.html)
for more details.

See the notes in the [Dockerfile](Dockerfile) about running the Jote tests.

This project must be built using Java 11.
