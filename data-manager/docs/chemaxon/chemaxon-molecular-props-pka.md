# Job: chemaxon-molecular-props-pka

This describes how to run the `chemaxon-molecular-props-pka` job from the `molecular properties` category in the `chemaxon` collection.

## What the job does

This job calculates the most acidic and most basic pKa values.
The pKa values are added to the output as additional fields with the field names `CXN_aPka` and `CXN_bPka`.

The jobs can handle SD-files or delimited text files (e.g. tab separated) as input and output.
When using delimited text files the molecules are read and written as SMILES.

## Implementation details

* Job implementation: [PKaCalc.java](/app/src/main/java/squonk/jobs/chemaxon/PKaCalc.java)
* Job definition: `jobs.chemaxon-molecular-props-pka` in [molprops.yaml](/data-manager/molprops.yaml)

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
* **acidic**: Calculate the most acidic pKa.
* **basic**: Calculate the most basic pKa.

## Outputs

The file specified by the *Output file* option is created containing all the original fields plus additional ones for 
the calculated properties.
The type of file is determined from the file extension, `.sdf` for SD file, `.smi` for delimited text.

