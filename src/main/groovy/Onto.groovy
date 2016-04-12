/*
 *    Copyright (c) Dilvan A. Moreira 2016. All rights reserved.
 *    This file is part of ePad.
 *
 *     ePad is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     ePad is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ePad.  If not, see <http://www.gnu.org/licenses/>.
 */


import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.io.*
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.util.*
import org.semanticweb.owlapi.vocab.*

import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory

@CompileStatic
class Onto {
    OWLOntologyManager manager
    OWLDataFactory factory
    DefaultPrefixManager prefix
    OWLOntology onto

    Onto(String iri = null) {
        manager = OWLManager.createOWLOntologyManager()
        factory = manager.OWLDataFactory
        prefix = new DefaultPrefixManager()
        if (iri)
            onto = manager.createOntology(IRI.create(iri))
    }

    def propertyMissing(String name) { println 'prop= ' + name; name }

    def Prefix(uri) { prefix.defaultPrefix = uri}

    def Prefix(prefixName, String iri) { prefix.setPrefix(prefixName.toString(), iri)}

    IRI toIri(iri) {
        def iriName = iri.toString()
        if (iriName.startsWith("http:"))
            IRI.create(iriName)
        else
            prefix.getIRI(iriName)
    }

    def run(Closure scp) {
        scp.delegate = this
        scp()
    }

    OWLClass Class(iri) {
        (iri instanceof OWLClass) ? (OWLClass) iri : factory.getOWLClass(toIri(iri))
    }

    OWLObjectProperty ObjectProperty(iri) {
        (iri instanceof OWLObjectProperty) ? (OWLObjectProperty) iri :
                factory.getOWLObjectProperty(toIri(iri))
    }

    OWLDataProperty DataProperty(iri) {
        (iri instanceof OWLDataProperty) ? (OWLDataProperty) iri :
                factory.getOWLDataProperty(toIri(iri))
    }

    OWLAnnotationProperty AnnotationProperty(iri) {
        (iri instanceof OWLAnnotationProperty) ? (OWLAnnotationProperty) iri :
                factory.getOWLAnnotationProperty(toIri(iri))
    }

    OWLAnonymousIndividual owlIndividual() {
        factory.getOWLAnonymousIndividual()
    }

    OWLIndividual owlIndividual(iri) {
        if (iri instanceof OWLIndividual) return (OWLIndividual) iri;
        if (iri.toString().startsWith('_:'))
            factory.getOWLAnonymousIndividual(iri.toString())
        else factory.getOWLNamedIndividual(toIri(iri))
    }

    def NamedIndividual(iri) {
        (iri instanceof OWLNamedIndividual) ? (OWLNamedIndividual) iri :
                factory.getOWLNamedIndividual(toIri(iri))
    }

    def Declaration(entity) {
        manager.addAxiom(onto, factory.getOWLDeclarationAxiom((OWLEntity) entity))
    }

    def objectPropertyAssertion(OWLObjectProperty prop, OWLIndividual subj, OWLIndividual obj) {
        manager.addAxiom(onto, factory.getOWLObjectPropertyAssertionAxiom(prop, subj, obj))
    }

    def ObjectPropertyAssertion(prop, subj, obj) {
        objectPropertyAssertion(
                ObjectProperty(prop),
                owlIndividual(subj),
                owlIndividual(obj)
        )
    }

    def toXSD(xsd) {
        switch (xsd.toString()) {
            case 'xsd:string': return OWL2Datatype.XSD_STRING
            case 'xsd:int': return OWL2Datatype.XSD_INT
            case 'xsd:integer': return OWL2Datatype.XSD_INTEGER
            case 'xsd:float': return OWL2Datatype.XSD_FLOAT
            case 'xsd:double': return OWL2Datatype.XSD_DOUBLE
            case 'xsd:dateTime': return OWL2Datatype.XSD_DATE_TIME
            case 'xsd:anyURI': return OWL2Datatype.XSD_ANY_URI

            case 'xsd:date': return factory.getOWLDatatype(XSDVocabulary.DATE.IRI)
            case 'xsd:gYear': return factory.getOWLDatatype(XSDVocabulary.G_YEAR.IRI)
            case 'xsd:gYearMonth': return factory.getOWLDatatype(XSDVocabulary.G_YEAR_MONTH.IRI)
            default: xsd
        }
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    OWLLiteral makeXsd(obj, xsd) {
        factory.getOWLLiteral(obj.toString(), toXSD(xsd))
    }

    //		"Creates a OWLLiteral of type Date"
    def dateLiteral(String year, String month, String day) {
        try {
            def gYear = Integer.parseInt(year)
            def gMonth = (month == "") ? DatatypeConstants.FIELD_UNDEFINED : Integer.parseInt(month)
            def gDay = (month == "" || day == "") ? DatatypeConstants.FIELD_UNDEFINED : Integer.parseInt(day)
            def iri = (month == "") ? XSDVocabulary.G_YEAR : ((day == "") ? XSDVocabulary.G_YEAR_MONTH : XSDVocabulary.DATE)
            def date = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(gYear, gMonth, gDay, DatatypeConstants.FIELD_UNDEFINED).toXMLFormat()
            def dataType = factory.getOWLDatatype(iri.IRI)
            factory.getOWLLiteral(date, dataType)
        }
        catch (e) {
            null
        }
    }

    def dataPropertyAssertion(OWLDataPropertyExpression prop, OWLIndividual subj, OWLLiteral obj) {
        manager.addAxiom(onto, factory.getOWLDataPropertyAssertionAxiom(prop, subj, obj))
    }

    def DataPropertyAssertion(prop, subj, obj, xsd) {
        dataPropertyAssertion(
                DataProperty(prop),
                owlIndividual(subj),
                makeXsd(obj, xsd)
        )
    }

    def annotationAssertion(OWLAnnotationProperty prop, OWLAnnotationSubject subj, OWLAnnotationValue obj) {
        manager.addAxiom(onto, factory.getOWLAnnotationAssertionAxiom(subj, factory.getOWLAnnotation(prop, obj)))
    }


    //@TypeChecked(TypeCheckingMode.SKIP)
    def AnnotationAssertion(prop, subj, obj) {
        annotationAssertion(
                AnnotationProperty(prop),
                (OWLAnnotationSubject) ((subj instanceof String) ? toIri(subj) : subj),
                (OWLAnnotationValue) ((obj instanceof String) ? toIri(obj) : obj)
        )
    }

    //@TypeChecked(TypeCheckingMode.SKIP)
    def AnnotationAssertion(prop, subj, obj, xsd) {
        annotationAssertion(
                AnnotationProperty(prop),
                (OWLAnnotationSubject) ((subj instanceof String) ? toIri(subj) : subj),
                makeXsd(obj, xsd)
        )
    }

    def classAssertion(OWLClass cls, OWLIndividual individual) {
        manager.addAxiom(onto, factory.getOWLClassAssertionAxiom(cls, individual))
    }

    def ClassAssertion(cls, individual) {
        classAssertion(
                Class(cls),
                owlIndividual(individual))
    }

    def subclassOf(OWLClassExpression subclass, OWLClassExpression cls) {
        manager.addAxiom(onto, factory.getOWLSubClassOfAxiom(subclass, cls))
    }

    def subpropertyOf(OWLPropertyExpression subProperty, OWLPropertyExpression property) {
        if (property.isObjectPropertyExpression()) {
            manager.addAxiom(onto, factory.getOWLSubObjectPropertyOfAxiom( (OWLObjectPropertyExpression) subProperty, (OWLObjectPropertyExpression) property))
        }
        //manager.addAxiom(onto, factory.getOWLSubClassOfAxiom(subclass, cls))
    }

    //def subpropertyOf(OWLAnnotationProperty prop, OWLAnnotationSubject subj, OWLAnnotationValue obj) {
    //    manager.addAxiom(onto, factory.getOWLAnnotationAssertionAxiom(subj, factory.getOWLAnnotation(prop, obj)))
    //}


    def SubclassOf(subclass, cls) {
        subclassOf(
                Class(subclass),
                Class(cls))
    }

    def save(IRI file) {
        manager.saveOntology(onto, new OWLFunctionalSyntaxOntologyFormat(), file)
    }

    def save(String file, formatType) {
        def ontFormat = manager.getOntologyFormat(onto)
        def saveFormat
        switch (formatType) {
            case 'functional': saveFormat= new OWLFunctionalSyntaxOntologyFormat(); break
            case 'manchester': saveFormat= new ManchesterOWLSyntaxOntologyFormat(); break
            default: saveFormat= new OWLXMLOntologyFormat()

        }
        if (ontFormat.isPrefixOWLOntologyFormat())
            saveFormat.copyPrefixesFrom(ontFormat.asPrefixOWLOntologyFormat())
        saveFormat.copyPrefixesFrom(prefix)
        manager.saveOntology(onto, saveFormat, IRI.create(new File(file).toURI()))
    }

    def importOnt(String name, String file) {
        manager.applyChange(
                new AddImport(onto, factory.getOWLImportsDeclaration(IRI.create(name))))
        manager.loadOntologyFromOntologyDocument(new File(file))

    }

    def mergeLoadedOntologies(String iri) {
        def merger = new OWLOntologyMerger(manager)
        onto = merger.createMergedOntology(manager, IRI.create(iri))
    }

    def doIt(@DelegatesTo(Onto) Closure code) {
        code.delegate = this;
        code()
    }

    def loadOntology(IRI file) {
        //def onto = new Onto()
        def loadFromDoc = { manager.loadOntologyFromOntologyDocument(file) }
        try {
            onto = loadFromDoc()
        }
        catch (OWLOntologyAlreadyExistsException e) {
            manager.removeOntology(e.ontologyID)
            onto = loadFromDoc()
        }
        //onto
    }

    static toUri(String s) {
        URLEncoder.encode(s.replace("(", "").replace(")", "").replace(' ', '_'))
    }

}
