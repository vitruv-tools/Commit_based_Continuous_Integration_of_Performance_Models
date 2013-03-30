/**
 */
package org.splevo.diffing.emfcompare.java2kdmdiff.impl;

import org.eclipse.emf.compare.diff.metamodel.DifferenceKind;
import org.eclipse.emf.ecore.EClass;

import org.splevo.diffing.emfcompare.java2kdmdiff.Java2KDMDiffPackage;
import org.splevo.diffing.emfcompare.java2kdmdiff.StatementDelete;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Statement Delete</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * </p>
 *
 * @generated
 */
public class StatementDeleteImpl extends StatementChangeImpl implements StatementDelete {
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected StatementDeleteImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    protected EClass eStaticClass() {
        return Java2KDMDiffPackage.Literals.STATEMENT_DELETE;
    }

    /**
     * <!-- begin-user-doc -->
     * The difference kind of a statement delete is always DifferenceKind.DELETION.
     * <!-- end-user-doc -->
     * {@inheritDoc}
     * @generated NOT
     */
    @Override
    public DifferenceKind getKind() {
        return DifferenceKind.DELETION;
    }

} //StatementDeleteImpl
