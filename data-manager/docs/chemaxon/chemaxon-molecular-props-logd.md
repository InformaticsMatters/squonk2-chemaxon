# Job: chemaxon-molecular-props-logd

This describes how to run the `chemaxon-molecular-props-logd` job from the `molecular properties` category in the `chemaxon` collection.

## What the job does

This job calculates logD. The pH can be specified along with threshold values for filtering out molcules.
The logD is added to the output as an additional field with the field name `CXN_cLogP`.

The jobs can handle SD-files or delimited text files (e.g. tab separated) as input and output.
When using delimited text files the molecules are read and written as SMILES.

## Implementation details

* Job implementation: [LogDCalc.java](java/squonk/jobs/chemaxon/LogDCalc.java)
* Job definition: `jobs.chemaxon-molecular-props-logd` in [molprops.yaml](/data-manager/molprops.yaml)

## How to run the job

### Inputs

* **Input molecules**: The molecules to calculate, in SDF or delimited text files.
  This uses ChemAxon's [MolImporter's](https://apidocs.chemaxon.com/jchem/doc/dev/java/api/chemaxon/formats/MolImporter.html)
  automatic file format detection to detect the type of file. SD-file and tab delimited text with SMILES as the first column
  should work, along with some other formats. If the tab delimited text file contains a header line MolImporter *should*
  detect this.

### Options

* **Output file**: The name of the output file. The format to output is determined using the file extension, `.sdf` or .smi.
* **Include header**: when writing delimited text files writer the first line as a header line containing the field names.
* **pH**: The pH to use.
* **Min value**: The lower bound for the threshold (optional).
* **Max value**: The upper bound for the threshold (optional).

## Outputs

The file specified by the *Output file* option is created containing all the original fields plus additional ones for 
the calculated properties.
The type of file is determined from the file extension, `.sdf` for SD file, `.smi` for delimited text.


## Related topics

* [chemaxon-molecular-props-simple](chemaxon-molecular-props-simple.md) simple calculator for several properties.
