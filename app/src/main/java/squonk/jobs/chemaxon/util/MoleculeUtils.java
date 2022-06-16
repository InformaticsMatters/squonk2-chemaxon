/*
 *  Copyright (c) 2022  Informatics Matters Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package squonk.jobs.chemaxon.util;


import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

public class MoleculeUtils {

    public static Molecule createRepresentation(Molecule mol, MoleculeObject.Representation key) {

        switch (key) {
            case Original:
                return mol.clone();
            case ExplicitH:
                return moleculeWithExplicitHydrogens(mol);
            case ImplicitH:
                return moleculeWithImplicitHydrogens(mol);
            default:
                throw new UnsupportedOperationException("Can't handle representation " + key);
        }
    }

    public static Molecule moleculeWithExplicitHydrogens(Molecule mol) {
        MolHandler h = new MolHandler(mol.clone());
        h.addHydrogens();
        return h.getMolecule();
    }

    public static Molecule moleculeWithImplicitHydrogens(Molecule mol) {
        MolHandler h = new MolHandler(mol.clone());
        h.removeHydrogens();
        return h.getMolecule();
    }

    public static Stream<MoleculeObject> readMoleculesAsStream(String path) throws IOException {
        File file = new File(path);
        MolImporter importer = new MolImporter(file);
        Stream<Molecule> mols = importer.getMolStream();
        return mols.map(m -> new MoleculeObject(m));
    }
}
