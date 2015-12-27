/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.connect.packages.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.p2.cudf.Parser;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;
import org.eclipse.equinox.p2.cudf.solver.SimplePlanner;
import org.eclipse.equinox.p2.cudf.solver.SolverConfiguration;

import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.Version;

/**
 * This implementation uses the p2cudf resolver to solve complex dependencies
 *
 * @since 1.4
 */
public class P2CUDFDependencyResolver implements DependencyResolver {

    protected static Log log = LogFactory.getLog(P2CUDFDependencyResolver.class);

    protected PackageManager pm;

    protected CUDFHelper cudfHelper;

    protected P2CUDFDependencyResolver() {
    }

    public P2CUDFDependencyResolver(PackageManager pm) {
        this.pm = pm;
    }

    @Override
    public DependencyResolution resolve(List<String> pkgInstall,
            List<String> pkgRemove, List<String> pkgUpgrade,
            String targetPlatform) throws DependencyException {
        return resolve(pkgInstall, pkgRemove, pkgUpgrade, targetPlatform,
                CUDFHelper.defaultAllowSNAPSHOT);
    }

    @Override
    public DependencyResolution resolve(List<String> pkgInstall,
            List<String> pkgRemove, List<String> pkgUpgrade,
            String targetPlatform, boolean allowSNAPSHOT)
            throws DependencyException {
        return resolve(pkgInstall, pkgRemove, pkgUpgrade, targetPlatform,
                allowSNAPSHOT, true);
    }

    @Override
    public DependencyResolution resolve(List<String> pkgInstall,
            List<String> pkgRemove, List<String> pkgUpgrade,
            String targetPlatform, boolean allowSNAPSHOT, boolean doKeep)
            throws DependencyException {
        cudfHelper = new CUDFHelper(pm);
        cudfHelper.setTargetPlatform(targetPlatform);
        cudfHelper.setAllowSNAPSHOT(allowSNAPSHOT);
        cudfHelper.setKeep(doKeep);
        // generate CUDF package universe and request stanza
        String cudf = cudfHelper.getCUDFFile(str2PkgDep(pkgInstall),
                str2PkgDep(pkgRemove), str2PkgDep(pkgUpgrade));
        log.debug("CUDF request:\n" + cudf);

        // pass to p2cudf for solving
        ProfileChangeRequest req = new Parser().parse(IOUtils.toInputStream(cudf));
        SolverConfiguration configuration = new SolverConfiguration(
                SolverConfiguration.OBJ_ALL_CRITERIA);
        // Upgrade + verbose + explain is unsupported
        // verbose + explain changes results
        // if (log.isTraceEnabled()) {
        // configuration.verbose = true;
        // configuration.explain = true;
        // }
        SimplePlanner planner = new SimplePlanner();
        planner.getSolutionFor(req, configuration);
        planner.stopSolver();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Hazardous wait for the solver stop
        }
        Collection<InstallableUnit> solution = planner.getBestSolutionFoundSoFar();
        if (log.isTraceEnabled()) {
            log.trace(planner.getExplanation());
        }
        if (!planner.isSolutionOptimal()) {
            log.warn("The solution found might not be optimal");
        }
        DependencyResolution resolution = cudfHelper.buildResolution(solution,
                planner.getSolutionDetails());
        if (!doKeep) {
            // Make sub-resolution to remove all packages that are not part of
            // our target list
            List<String> subInstall = new ArrayList<>();
            List<String> subRemove = new ArrayList<>();
            for (Map.Entry<String, Version> e : resolution.localPackagesToInstall.entrySet()) {
                subInstall.add(e.getKey() + '-' + e.getValue().toString());
            }
            for (Map.Entry<String, Version> e : resolution.localUnchangedPackages.entrySet()) {
                subInstall.add(e.getKey() + '-' + e.getValue().toString());
            }
            for (Map.Entry<String, Version> e : resolution.newPackagesToDownload.entrySet()) {
                subInstall.add(e.getKey() + '-' + e.getValue().toString());
            }
            for (DownloadablePackage pkg : pm.listInstalledPackages()) {
                String pkgId = pkg.getId();
                if (!subInstall.contains(pkgId)) {
                    subRemove.add(pkgId);
                }
            }
            resolution = resolve(subInstall, subRemove, null, targetPlatform,
                    allowSNAPSHOT, true);
        }
        return resolution;
    }

    private PackageDependency[] str2PkgDep(List<String> pkgList) {
        List<PackageDependency> list = new ArrayList<>();
        if (pkgList == null || pkgList.size() == 0) {
            return list.toArray(new PackageDependency[0]);
        }
        Map<String, DownloadablePackage> packagesByID = pm.getAllPackagesByID();
        for (String pkgStr : pkgList) {
            if (packagesByID.containsKey(pkgStr)) {
                DownloadablePackage pkg = packagesByID.get(pkgStr);
                list.add(new PackageDependency(pkg.getName(), pkg.getVersion(),
                        pkg.getVersion()));
            } else {
                list.add(new PackageDependency(pkgStr));
            }
        }
        return list.toArray(new PackageDependency[list.size()]);
    }

    @Override
    public DependencyResolution resolve(String pkgIdOrName,
            String targetPlatform) throws DependencyException {
        List<String> pkgInstall = new ArrayList<>();
        pkgInstall.add(pkgIdOrName);
        if (pm.isInstalled(pkgIdOrName)
                || !pm.findLocalPackageInstalledVersions(pkgIdOrName).isEmpty()) {
            // upgrade
            return resolve(null, null, pkgInstall, targetPlatform);
        } else {
            // new install
            return resolve(pkgInstall, null, null, targetPlatform);
        }
    }

}
