package cipm.consistency.designtime.instrumentation2;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emftext.language.java.members.Method;
import org.emftext.language.java.statements.Statement;
import org.emftext.language.java.statements.StatementListContainer;

import cipm.consistency.base.models.instrumentation.InstrumentationModel.ActionInstrumentationPoint;
import cipm.consistency.base.models.instrumentation.InstrumentationModel.InstrumentationModel;
import cipm.consistency.base.models.instrumentation.InstrumentationModel.InstrumentationType;
import cipm.consistency.base.models.instrumentation.InstrumentationModel.ServiceInstrumentationPoint;
import cipm.consistency.designtime.instrumentation2.instrumenter.MinimalMonitoringEnvironmentModelGenerator;
import cipm.consistency.designtime.instrumentation2.instrumenter.ServiceInstrumentationPointInstrumenter;
import tools.vitruv.framework.correspondence.CorrespondenceModel;
import tools.vitruv.framework.correspondence.CorrespondenceModelUtil;

/**
 * An instrumenter for the source code based on the instrumentation points in
 * the instrumentation model.
 * 
 * @author Martin Armbruster
 */
public final class CodeInstrumenter {
	private static final Logger LOGGER = Logger.getLogger("cipm." + CodeInstrumenter.class.getSimpleName());

	private CodeInstrumenter() {
	}

	public static Resource instrument(InstrumentationModel im, CorrespondenceModel cm, Resource javaModel, Path output,
			Path input, boolean adaptive) {
		LOGGER.debug("Executing the " + (adaptive ? "adaptive" : "full") + " instrumentation.");
		LOGGER.debug("Copying the Java model.");
		ResourceSet targetSet = new ResourceSetImpl();
		Resource copy = targetSet.createResource(javaModel.getURI());
		copy.getContents().addAll(EcoreUtil.copyAll(javaModel.getContents()));

		LOGGER.debug("Generating the minimal monitoring environment.");
		MinimalMonitoringEnvironmentModelGenerator gen = new MinimalMonitoringEnvironmentModelGenerator(copy);
		ServiceInstrumentationPointInstrumenter sipIns = new ServiceInstrumentationPointInstrumenter(gen);

		for (ServiceInstrumentationPoint sip : im.getPoints()) {
			LOGGER.debug("Instrumenting the service " + sip.getService().getDescribedService__SEFF().getEntityName());
			Method service = CorrespondenceModelUtil.getCorrespondingEObjects(cm, sip.getService(), Method.class)
					.iterator().next();
			Method copiedService = findCopiedEObject(targetSet, service);
			ActionStatementMapping statementMap = createActionStatementMapping(targetSet, sip, cm);
			sipIns.instrument(copiedService, sip, statementMap, adaptive);
		}

		LOGGER.debug("Saving the instrumented code.");
		ModelSaverInRepositoryCopy.saveModels(targetSet, copy, output, input, gen);
		LOGGER.debug("Finished the instrumentation.");

		return copy;
	}

	private static ActionStatementMapping createActionStatementMapping(ResourceSet copyContainer,
			ServiceInstrumentationPoint sip, CorrespondenceModel cm) {
		ActionStatementMapping statementMap = new ActionStatementMapping();
		for (ActionInstrumentationPoint aip : sip.getActionInstrumentationPoints()) {
			Set<Statement> correspondingStatements = CorrespondenceModelUtil.getCorrespondingEObjects(cm,
					aip.getAction(), Statement.class);
			Statement firstStatement;
			if (aip.getType() == InstrumentationType.INTERNAL || aip.getType() == InstrumentationType.INTERNAL_CALL) {
				Statement lastStatement = findFirstOrLastStatement(correspondingStatements, false);
				statementMap.getAbstractActionToLastStatementMapping().put(aip.getAction(),
						findCopiedEObject(copyContainer, lastStatement));
				firstStatement = findFirstOrLastStatement(correspondingStatements, true);
			} else {
				try {
					firstStatement = correspondingStatements.iterator().next();
				} catch (NoSuchElementException e) {
					continue;
				}
			}
			Statement copiedFirstStatement = findCopiedEObject(copyContainer, firstStatement);
			statementMap.put(aip.getAction(), copiedFirstStatement);
		}
		return statementMap;
	}

	@SuppressWarnings("unchecked")
	private static <T extends EObject> T findCopiedEObject(ResourceSet copyContainer, T original) {
		EObject potResult = copyContainer.getEObject(EcoreUtil.getURI(original), false);
		if (potResult != null && original.eClass().isInstance(potResult)) {
			return (T) potResult;
		}
		return null;
	}

	private static Statement findFirstOrLastStatement(Set<Statement> statements, boolean findFirst) {
		if (statements.size() == 1) {
			return statements.iterator().next();
		}
		EObject container = statements.iterator().next().eContainer();
		if (container instanceof StatementListContainer) {
			EList<Statement> stats = ((StatementListContainer) container).getStatements();
			if (findFirst) {
				for (Statement next : stats) {
					if (statements.contains(next)) {
						return next;
					}
				}
			}
			Set<Statement> copiedStatements = new HashSet<>(statements);
			for (Statement next : stats) {
				copiedStatements.remove(next);
				if (copiedStatements.size() == 1) {
					return copiedStatements.iterator().next();
				}
			}
		}
		return null;
	}
}
