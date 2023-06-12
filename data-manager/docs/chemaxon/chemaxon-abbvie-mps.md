# Job: chemaxon-abbvie-mps

This describes how to run the `chemaxon-abbvie-mps` job from the `molecular properties` category in the `chemaxon` collection.

## What the job does

This job calculates a multi-parametric score of drug-likeness derived at Abbvie.
See Beyond the Rule of 5: Lessons Learned from AbbVie’s Drugs and Compound Collection Miniperspective,
DeGoey *et al.*, J. Med. Chem. 2018, 61, 2636−2651. DOI: 10.1021/acs.jmedchem.7b00717.

The score is calculated as follows:

    abs(logD(7.4) - 3) + num_aromatic_rings + num_rotatable_bonds

Threshold values for filtering out molecules can also be specified.
The score is added to the output as an additional field with the field name `Abbvie_MPS`.

The job can handle SD-files or delimited text files (e.g. tab separated) as input and output.
When using delimited text files the molecules are read and written as SMILES.

## Implementation details

* Job implementation: [AbbvieMPSCalc.java](/app/src/main/java/squonk/jobs/chemaxon/AbbvieMPSCalc.java)
* Job definition: `jobs.chemaxon-abbvie-mps` in [molprops.yaml](/data-manager/molprops.yaml)

## How to run the job

**NOTE**: to run this job you must have a ChemAxon license file registered as an asset with the name
`chemaxon-license-file` in the Account server.

### Inputs

* **Input molecules**: The molecules to calculate, in SDF or delimited text files.
  This uses ChemAxon's [MolImporter's](https://apidocs.chemaxon.com/jchem/doc/dev/java/api/chemaxon/formats/MolImporter.html)
  automatic file format detection to detect the type of file. SD-file and tab delimited text with SMILES as the first column
  should work, along with some other formats. If the tab delimited text file contains a header line MolImporter *should*
  detect this.

### Options

* **Output file**: The name of the output file. The format to output is determined using the file extension, `.sdf` or .smi.
* **Include header**: when writing delimited text files writer the first line as a header line containing the field names.
* **mode**: The filter mode to use. Must be set to pass or fail if filter values are to be used.
* **Min value**: The lower bound for the threshold (optional).
* **Max value**: The upper bound for the threshold (optional).

## Outputs

The file specified by the *Output file* option is created containing all the original fields plus additional ones for 
the calculated properties.
The type of file is determined from the file extension, `.sdf` for SD file, `.smi` for delimited text.


## Related topics

* [chemaxon-molecular-props-simple](chemaxon-molecular-props-simple.md) simple calculator for several properties.
