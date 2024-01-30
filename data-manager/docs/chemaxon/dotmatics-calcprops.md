# Job: dotmatics-calcprops

This describes how to run the `dotmatics-calcprops` job from the `molecular properties` category in the `chemaxon` collection.

## What the job does

This job calculates a number of molecular properties for importing into Dotmatics. 

The calculated properties are:
* Identifier - the ID of the input molecule
* LogP - cLogP
* LogD7.4 - logD at pH 7.4
* CNS_MPO - Pfizer CNS MPO score (see [chemaxon-pfizer-cns-mpo.md](chemaxon-pfizer-cns-mpo.md))
* Acidic_pKa_1 - first acidic pKa
* Acidic_pKa_2 - second acidic pKa
* Basic_pKa_1 - first basc pKa
* Basic_pKa_2 - second basic pKa
* Chiral_centers - number of chiral centres
* Aromatic_rings - number of aromatic rings
* fsp3 - fraction of sp3 hybridised carbon atoms
* InChIKey - InChi key

Additional empty fields are added for compatibility.

## Implementation details

* Job implementation: [SygCalcs.java](/app/src/main/java/squonk/jobs/chemaxon/SygCalcs.java)
* Job definition: `jobs.dotmatics-calcprops` in [sygonly.yaml](/data-manager/sygonly.yaml)

## How to run the job

**NOTE**: to run this job you must have a ChemAxon license file registered as an asset with the name
`chemaxon-license-file` in the Account server.

### Inputs

* **Input molecules**: The molecules to calculate, in a CSV file. Format must be ID,SMILES with a header line.

### Options

This job has no user definable options.

## Outputs

The file specified by the *Output file* option is created containing the calculated properties and padding fields
described above. The file is in CSV format.


## Related topics

* [chemaxon-molecular-props-simple](chemaxon-molecular-props-simple.md) simple calculator for several properties
* [chemaxon-pfizer-cns-mpo.md](chemaxon-pfizer-cns-mpo.md) - CNS MPO calculator job
