/*
 *  Copyright (c) 2023  Informatics Matters Ltd.
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


import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

import squonk.jobs.chemaxon.SimpleCalcs;
import squonk.jobs.chemaxon.util.DMLogger;

public class MoleculeUtils {

    private static final Logger LOG = Logger.getLogger(MoleculeUtils.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

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

    public static Stream<MoleculeObject> addFileWriter(Stream<MoleculeObject> mols, String outputFile, boolean includeHeader)
            throws IOException {
        Path path = Paths.get(outputFile);
        Path dir = path.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }

        String opts = null;
        if (outputFile.endsWith(".sdf")) {
            opts = "sdf";
        } else if (outputFile.endsWith(".smi")) {
            if (includeHeader) {
                opts = "smiles:T*";
            } else {
                opts = "smiles:-T*";
            }
        } else {
            throw new IllegalArgumentException("Unsupported output file format " + outputFile);
        }
        // In principle this should work for gzipping, but you end up with empty files. ChemAxon bug?
//        if (outputFile.endsWith(".gz")) {
//            opts = "gzip:" + opts;
//        }
        LOG.info("Options: " + opts);
        final MolExporter exporter = new MolExporter(outputFile, opts);
        final AtomicInteger i = new AtomicInteger(0);
        Stream<MoleculeObject> str = mols.peek(mo -> {
            try {
                i.incrementAndGet();
                exporter.write(mo.getMol());
            } catch (IOException e) {
                DMLOG.logEvent(DMLogger.Level.WARNING, "Failed to export molecule " + i);
            }
        }).onClose(() -> {
            try {
                exporter.close();
            } catch (IOException e) {
                DMLOG.logEvent(DMLogger.Level.WARNING, "Failed to close MolExporter " + e.getMessage());
            }
        });

        return str;
    }
}
