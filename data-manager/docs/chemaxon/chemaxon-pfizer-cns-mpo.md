# Job: chemaxon-pfizer-cns-mpo

This describes how to run the `chemaxon-pfizer-cns-mpo` job from the `molecular properties` category in the `chemaxon` collection.

## What the job does

This job calculates a multi-parametric score of CNS likeness derived at Pfizer.
See:
- Moving beyond rules: The development of a central nervous system multiparameter optimization (CNS MPO) approach to 
  enable alignment of druglike properties. ACS Chem. Neurosci. 2010, 1, 435– 449. DOI: 10.1021/cn100008c
- Central Nervous System Multiparameter Optimization Desirability: Application in Drug Discovery
  Wager et al., ACS Chem. Neurosci. 2016, 7, 6, 767–775. DOI: 10.1021/acschemneuro.6b00029

The score is calculated as a multiparameteric combination of predicted values for logP, logD, MW, TPSA, HBD and bPka.
Each calculation is passed through the MPO scoring for that property, resulting in a best score of 1 and a worst of 0.
The resulting score is the sum of the six scores, with a maximum value of 6 and minimum of 0. See the 2010 paper for 
full details.

Note that these calculations are performed using ChemAxon's predictors whereas those in the Pfizer paper used other
predictors (notably Biobyte for logP and ACD Labs for logD and pKa).

Threshold values for filtering out molecules can also be specified.
The score is added to the output as an additional field with the field name `Pfizer_CNS_MPO`.

The job can handle SD-files or delimited text files (e.g. tab separated) as input and output.
When using delimited text files the molecules are read and written as SMILES.

## Implementation details

* Job implementation: [PfizerCNSMPOCalc.java](/app/src/main/java/squonk/jobs/chemaxon/PfizerCNSMPOCalc.java)
* Job definition: `jobs.chemaxon-pfizer-cns-mpo` in [molprops.yaml](/data-manager/molprops.yaml)

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
