# Job: chemaxon-molecular-props-simple

This describes how to run the `chemaxon-molecular-props-simple` job from the `molecular properties` category in the `chemaxon` collection.

## What the job does

This job calculates a range of molecular properties using ChemAxon tools.
These properties are all "simple" properties with no parameters, or using sensible default parameters.
Some properties have more specialist jobs where parameters can be specified.

These properties (with the corresponding field names in brackets) are:
- Molecular weight (CXN_molecularWeight)
- Molecular formula (CXN_molecularFormula)
- Atom count (CXN_atomCount)
- Heavy atom count (CXN_heavyAtomCount)
- Bond count (CXN_bondCount)
- LogP (CXN_cLogP)
- LogD at pH 7.4 (CXN_cLogP)
- H-bond donor count (CXN_donorCount)
- H-bond acceptor count (CXN_acceptorCount)
- H-bond donor sites (CXN_donorSites)
- H-bond acceptor sites (CXN_acceptorSites)
- Ring count (CXN_ringCount)
- Ring atom count (CXN_ringAtomCount)
- Aromatic ring count (CXN_aromaticRingCount)
- Aromatic atom count (CXN_aromaticAtomCount)
- Rotatable bond count (CXN_rotatableBondCount)
- Topological polar surface area (CXN_tpsa)

The jobs can handle SD-files or delimited text files (e.g. tab separated) as input and output.
When using delimited text files the molecules are read and written as SMILES.

## Implementation details

* Job implementation: [SimpleCalcs.java](/app/src/main/java/squonk/jobs/chemaxon/SimpleCalcs.java)
* Job definition: `jobs.chemaxon-molecular-props-simple` in [molprops.yaml](/data-manager/molprops.yaml)

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
* **Calculate XYZ**: An checkbox for each of the calculated properties listed above.

## Outputs

The file specified by the *Output file* option is created containing all the original fields plus additional ones for 
the calculated properties.
The type of file is determined from the file extension, `.sdf` for SD file, `.smi` for delimited text.


## Related topics

* [chemaxon-molecular-props-logd](chemaxon-molecular-props-logd.md) Specialist calculator for logD.