//#!/usr/bin/env groovy
//
//@Grab('net.sourceforge.owlapi:owlapi-distribution:3.4.10')
//@Grab('org.yaml:snakeyaml:1.17')
//@Grab('com.github.albaker:GroovySparql:0.9.0')

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.semanticweb.owlapi.io.OWLOntologyCreationIOException
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException
import org.semanticweb.owlapi.model.OWLOntologyCreationException
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException
import org.semanticweb.owlapi.model.UnloadableImportException

import static groovy.transform.TypeCheckingMode.SKIP
import groovy.sparql.Sparql
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat
import org.semanticweb.owlapi.model.AddImport
import org.semanticweb.owlapi.model.EntityType
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLAnnotationSubject
import org.semanticweb.owlapi.model.OWLAnnotationValue
import org.semanticweb.owlapi.model.OWLClass
import org.semanticweb.owlapi.model.OWLClassExpression
import org.semanticweb.owlapi.model.OWLDataFactory
import org.semanticweb.owlapi.model.OWLDataProperty
import org.semanticweb.owlapi.model.OWLDataRange
import org.semanticweb.owlapi.model.OWLEntity
import org.semanticweb.owlapi.model.OWLIndividual
import org.semanticweb.owlapi.model.OWLNamedIndividual
import org.semanticweb.owlapi.model.OWLObjectProperty
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.util.DefaultPrefixManager
import org.semanticweb.owlapi.vocab.OWL2Datatype
import org.yaml.snakeyaml.Yaml


/**
 * Created by dilvan on 4/7/16.
 */

@CompileStatic
class Yaml2Owl {

    OWLOntologyManager manager
    OWLDataFactory factory
    DefaultPrefixManager prefix
    OWLOntology onto

    String baseIRI
    String format = 'manchester'

    def remap = [
            'label': 'rdfs:label',
    ]

    static final String OWL = 'http://www.w3.org/2002/07/owl#'
    static final String RDF = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#'
    static final String XML = 'http://www.w3.org/XML/1998/namespace'
    static final String XSD = 'http://www.w3.org/2001/XMLSchema#'
    static final String RDFS = 'http://www.w3.org/2000/01/rdf-schema#'

    Map map

    def reserved = ['prefix', 'ontology', 'includes', 'imports', 'map', 'addFrom', 'is_a', 'subPropertyOf',
                    'range', 'domain', 'functional', 'inverse', 'inverseFunctional', 'transitive',
                    'type', 'int', 'integer', 'real', 'double', 'float', 'string', 'uri', 'date']

    Yaml2Owl(String iri = null) {
        manager = OWLManager.createOWLOntologyManager()
        factory = manager.OWLDataFactory
        prefix = new DefaultPrefixManager()
        if (iri)
            onto = manager.createOntology(IRI.create(iri))
        map = [
                'label' : factory.RDFSLabel,
                'comment' : factory.RDFSComment,
                'AnnotationProperty' : factory.getOWLAnnotationProperty(IRI.create(OWL+'AnnotationProperty')),
                'DataProperty' : factory.getOWLDataProperty(IRI.create(OWL+'DataProperty')),
                'ObjectProperty' : factory.getOWLObjectProperty(IRI.create(OWL+'ObjectProperty')),
                'integer': factory.getOWLDatatype(IRI.create(XSD+'integer')),
                'real': factory.getOWLDatatype(IRI.create(OWL+'real')),
                'uri': factory.getOWLDatatype(IRI.create(XSD+'anyURI')),
                'Literal': factory.getOWLDatatype(IRI.create(RDFS+'Literal')),
                'double': factory.getOWLDatatype(IRI.create(XSD+'double')),
                'int': factory.getOWLDatatype(IRI.create(XSD+'int')),
                'string': factory.getOWLDatatype(IRI.create(XSD+'string'))
        ]
    }

    def IRI toIRI(String iri, String base = baseIRI){
        if (remap[iri])
            iri = remap[iri]
        if (iri.startsWith('http://'))
            return IRI.create(iri)
        if (iri.contains(':')) {
            def str = iri.split(':')
            return IRI.create(prefix.getPrefix(str[0]+':'), str[1])
        }
        IRI.create(base + iri)
    }

    OWLEntity getEntity(String iri, EntityType entType = null) {
        if (iri in reserved) {
            println "Error: $iri is a reserved word, you cannot use it as a class, property or individual name."
            return null
        }
        if (remap[iri])
            iri = remap[iri]
        if (map[iri])
            return map[iri] as OWLEntity
        def entities = onto.getEntitiesInSignature((IRI) toIRI(iri))
        if (!entities.empty)
            return entities[0]
        for (IRI imp : onto.directImportsDocuments){
            entities = manager.getOntology(imp).getEntitiesInSignature(toIRI(iri, imp.toString()))
            if (!entities.empty) return entities[0]
        }
        if (! entType) return null
        factory.getOWLEntity(entType, toIRI(iri))
    }

    def restriction(OWLClass subj, OWLDataProperty prop, OWLDataRange obj){
        def someValuesFrom = factory.getOWLDataSomeValuesFrom(prop, obj)
        manager.addAxiom(onto, factory.getOWLSubClassOfAxiom(subj, someValuesFrom))
    }

    def restriction(OWLClass subj, OWLObjectProperty prop, OWLClassExpression obj){
        def someValuesFrom = factory.getOWLObjectSomeValuesFrom(prop, obj)
        manager.addAxiom(onto, factory.getOWLSubClassOfAxiom(subj, someValuesFrom))
    }

    @TypeChecked(SKIP)
    def restriction(OWLClass subj, OWLObjectProperty prop, Collection individuals){
        restriction(subj, prop, factory.getOWLObjectOneOf((Set<OWLIndividual>) individuals.collect{ makeIndividual(it)}))
    }

    @TypeChecked(SKIP)
    def makeLiteral(obj) {

//        if (obj in Integer)
//            return factory.getOWLLiteral(obj.toString(), OWL2Datatype.XSD_INTEGER)
//        if (obj in Long)
//            return factory.getOWLLiteral(obj.toString(), OWL2Datatype.XSD_LONG)
//        if (obj in Float)
//            return factory.getOWLLiteral(obj.toString(), OWL2Datatype.XSD_FLOAT)
//        if (obj in Double)
//            return factory.getOWLLiteral(obj.toString(), OWL2Datatype.XSD_DOUBLE)
        if (obj in Integer || obj in Long)
            return factory.getOWLLiteral(obj.toString(), OWL2Datatype.XSD_INTEGER)
        if (obj in Float || obj in Double)
            return factory.getOWLLiteral(obj.toString(), OWL2Datatype.OWL_REAL)
        if (obj in String) {
            String str = obj.trim()
            def length = str.length()
            if (str[length-3] == '@')
                return factory.getOWLLiteral(str.substring(0, length-3).trim(), str.substring(length-2, length))
            else
                return factory.getOWLLiteral(str, '')
        }
        factory.getOWLLiteral(obj)
    }

    OWLIndividual individual(String iri) {
        //if (iri instanceof OWLIndividual) return (OWLIndividual) iri
        if (iri.toString().startsWith('_:'))
            return factory.getOWLAnonymousIndividual(iri.toString())
        if (iri)
            (OWLIndividual) getEntity(iri, EntityType.NAMED_INDIVIDUAL)
        else factory.getOWLAnonymousIndividual()
    }

    @TypeChecked(SKIP)
    def OWLIndividual makeIndividual(Map ind, String id = null){

        def indiv = id ? getEntity(id, EntityType.NAMED_INDIVIDUAL) : factory.getOWLAnonymousIndividual()

        ind.keySet().forEach {
            if (it == 'type'){
                def cls = getEntity(ind['type'])
                manager.addAxiom(onto, factory.getOWLClassAssertionAxiom(cls, indiv))
                return
            }
            def prop = getEntity(it)
            if (!prop) {
                println "Error: Unknown property: $it"
                return
            }
            if (prop.OWLDataProperty)
                if (ind[it] in Collection)
                    ind[it].forEach{
                        manager.addAxiom(onto, factory.getOWLDataPropertyAssertionAxiom(prop, indiv, makeLiteral(it)))
                    }
                else
                    manager.addAxiom(onto, factory.getOWLDataPropertyAssertionAxiom(prop, indiv, makeLiteral(ind[it])))

            else if (prop.OWLObjectProperty)
                if (ind[it] in Collection)
                    ind[it].forEach{
                        manager.addAxiom(onto, factory.getOWLObjectPropertyAssertionAxiom(prop, indiv, individual(it)))
                    }
                else
                    manager.addAxiom(onto, factory.getOWLObjectPropertyAssertionAxiom(prop, indiv, individual(ind[it])))
        }
        if (indiv in OWLNamedIndividual)
            addAnnotation(ind, indiv.IRI.toString())
        else
            addAnnotation(ind, indiv)
        indiv
    }

    @TypeChecked(SKIP)
    def OWLIndividual makeIndividual(String subj, String property, String obj){

       // println "tripla: $subj $property : $obj"
        def indiv = getEntity(subj, EntityType.NAMED_INDIVIDUAL)

        if (property == 'type'){
            //println "Class: $obj"
            def cls = getEntity(obj)
            manager.addAxiom(onto, factory.getOWLClassAssertionAxiom(cls, indiv))
            return
        }
        def prop = getEntity(property)
        if (!prop) {
            println "Error: Unknown property: $property"
            return
        }
        if (prop.OWLDataProperty)
            manager.addAxiom(onto, factory.getOWLDataPropertyAssertionAxiom(prop, indiv, makeLiteral(obj)))
        else if (prop.OWLObjectProperty)
            manager.addAxiom(onto, factory.getOWLObjectPropertyAssertionAxiom(prop, indiv, individual(obj)))
        else
            annotate(subj, prop, obj)
        indiv
    }

    @TypeChecked(SKIP)
    def annotate(subject, prop, object){
        def obj = null
        if (object in String)
            obj = getEntity(object)

        if (subject in String) {
            def sub = subject
            subject = getEntity(subject)
            if (!subject) {
                println "Error: Entity $sub doesn't exist."
                return
            }
            subject = subject.IRI
        }
        if (obj)
            manager.addAxiom(onto, factory.getOWLAnnotationAssertionAxiom((OWLAnnotationSubject) subject, factory.getOWLAnnotation(prop, obj)))
        else
            manager.addAxiom(onto, factory.getOWLAnnotationAssertionAxiom((OWLAnnotationSubject) subject, factory.getOWLAnnotation(prop, (OWLAnnotationValue) makeLiteral(object))))
    }

    @TypeChecked(SKIP)
    def addAnnotation(Map ind, entity){

        ind.keySet().forEach {
            def prop = getEntity(it)

            //println "Entity: $it"
            if (!prop || !prop.OWLAnnotationProperty) return

            if (ind[it] in Collection) {
                ind[it].forEach{
                    annotate(entity, prop, it)
                }
            } else
                annotate(entity, prop, ind[it])
        }
    }

    def importOnt(String name, String file) {
        manager.applyChange(
                new AddImport(onto, factory.getOWLImportsDeclaration(IRI.create(name))))
        try {
            if (file) manager.loadOntologyFromOntologyDocument(new File(file))
            else manager.loadOntologyFromOntologyDocument(IRI.create(name))
        } catch (OWLOntologyCreationException e) {
            println "Error: There was a problem in creating and loading the ontology $name: ${e.message}"
        } catch (UnloadableImportException e) {
            println "Error: Ontology $name imports ontologies and one of this imports could not be loaded for what ever reason: $e.message"
        } catch (OWLOntologyCreationIOException e) {
            println "Error: There was an IOException when trying to load the ontology $name: $e.message"
        } catch (OWLOntologyDocumentAlreadyExistsException e) {
            println "Error: The specified documentIRI ($name) is already the document IRI for a loaded ontology: $e.message"
        } catch (OWLOntologyAlreadyExistsException e) {
            println "Error: The manager already contains an ontology whose ontology IRI and version IRI is the same as the ontology IRI ($name) and version IRI of the ontology contained in the document pointed to by documentIRI: $e.message."
        }
    }

    def save(String file, formatType = format) {
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
        try {
            manager.saveOntology(onto, saveFormat, IRI.create(new File(file).toURI()))
        } catch (e){
            println "Error: Ontology $file couldn't be saved."
        }
    }

    String parseElem(String ele, row){
        def elem1 = ele.trim()
        if (elem1[0] == '?') elem1 = row[elem1.substring(1)]
        elem1
    }

    def readYaml(Map yaml) {
        prefix.defaultPrefix = yaml.ontology

        prefix.setPrefix('owl:', 'http://www.w3.org/2002/07/owl#')
        prefix.setPrefix('rdf:', 'http://www.w3.org/1999/02/22-rdf-syntax-ns#')
        prefix.setPrefix('xml:', 'http://www.w3.org/XML/1998/namespace')
        prefix.setPrefix('xsd:', 'http://www.w3.org/2001/XMLSchema#')
        prefix.setPrefix('rdfs:', 'http://www.w3.org/2000/01/rdf-schema#')

        baseIRI = yaml.ontology

        readYaml2(yaml)
    }

    @TypeChecked(SKIP)
    def readYaml2(Map yaml) {

        yaml.keySet().forEach{ key ->

            if (key == 'ontology')
                return

            if (key == 'prefix') {
                yaml[key].keySet().forEach{
                    //println "$it --> ${yaml[key][it]}"
                    prefix.setPrefix(it+':', yaml[key][it])
                }
                return
            }

            if (key == 'includes') {
                yaml[key].forEach {
                    println "Reading $it ontology ..."

                    def yaml2 = new Yaml().load(new FileReader(it))
                    readYaml2(yaml2)

                    println "Finished (reading $it)."
                }
                return
            }


            if (key == 'imports') {
                yaml[key].forEach {
                    println "Reading ${it.file} ontology ..."

                    def yaml2 = new Yaml().load(new FileReader(it.file))
                    def onto = new Yaml2Owl(yaml2.ontology)
                    onto.readYaml(yaml2)

                    onto.save(it.file+'.owl', 'manchester')
                    importOnt(yaml2.ontology, it.file+'.owl')
                    prefix.setPrefix(it.prefix+':', yaml2.ontology)
                }
                return
            }
            if (key == 'OWLImports') {
                yaml[key].forEach {
                    importOnt(it.uri, it.file)
                    prefix.setPrefix(it.prefix+':', it.uri)
                }
                return
            }
            if (key == 'map') {
                remap.putAll(yaml[key])
                return
            }
            if (key == 'addFrom') {
               return
            }

            if (yaml[key].subPropertyOf) {

                //  SubPropertyOf
                OWLEntity prop = getEntity(yaml[key].subPropertyOf)

                //println "Sign: "+yaml[key].subPropertyOf+ " - " + prop
                if (!prop) {
                    println "Error: Superproperty ${yaml[key].subPropertyOf} doesn't exist."
                    return
                }

                if (prop.OWLAnnotationProperty) {
                    def annot = getEntity(key, EntityType.ANNOTATION_PROPERTY)
                    manager.addAxiom(onto, factory.getOWLDeclarationAxiom(annot))

                    if (prop != map['AnnotationProperty'])
                        manager.addAxiom(onto, factory.getOWLSubAnnotationPropertyOfAxiom(annot, prop))
                    if (yaml[key].domain)
                        manager.addAxiom(onto, factory.getOWLAnnotationPropertyDomainAxiom(annot, getEntity(yaml[key].domain, EntityType.CLASS).IRI))
                    if (yaml[key].range)
                        manager.addAxiom(onto, factory.getOWLAnnotationPropertyRangeAxiom(annot, getEntity(yaml[key].range, EntityType.CLASS).IRI))
                }
                else if (prop.OWLDataProperty) {
                    def data = getEntity(key, EntityType.DATA_PROPERTY)
                    manager.addAxiom(onto, factory.getOWLDeclarationAxiom(data))

                    if (prop != map['DataProperty'])
                        manager.addAxiom(onto, factory.getOWLSubDataPropertyOfAxiom(data, prop))
                    if (yaml[key].domain)
                        manager.addAxiom(onto, factory.getOWLDataPropertyDomainAxiom(data, getEntity(yaml[key].domain, EntityType.CLASS)))
                    if (yaml[key].range)
                        manager.addAxiom(onto, factory.getOWLDataPropertyRangeAxiom(data, getEntity(yaml[key].range, EntityType.DATATYPE)))
                    if (yaml[key].functional)
                        manager.addAxiom(onto, factory.getOWLFunctionalDataPropertyAxiom(data))
                }
                else if (prop.OWLObjectProperty) {
                    def obj = getEntity(key, EntityType.OBJECT_PROPERTY)
                    manager.addAxiom(onto, factory.getOWLDeclarationAxiom(obj))

                    if (prop != map['ObjectProperty'])
                        manager.addAxiom(onto, factory.getOWLSubObjectPropertyOfAxiom(obj, prop))
                    if (yaml[key].domain)
                        manager.addAxiom(onto, factory.getOWLObjectPropertyDomainAxiom(obj, getEntity(yaml[key].domain, EntityType.CLASS)))
                    if (yaml[key].range)
                        manager.addAxiom(onto, factory.getOWLObjectPropertyRangeAxiom(obj, getEntity(yaml[key].range, EntityType.CLASS)))
                    if (yaml[key].functional)
                        manager.addAxiom(onto, factory.getOWLFunctionalObjectPropertyAxiom(obj))
                    if (yaml[key].inverseFunctional)
                        manager.addAxiom(onto, factory.getOWLInverseFunctionalObjectPropertyAxiom(obj))
                    if (yaml[key].transitive)
                        manager.addAxiom(onto, factory.getOWLTransitiveObjectPropertyAxiom(obj))
                    if (yaml[key].inverseOf)
                        manager.addAxiom(onto, factory.getOWLInverseObjectPropertiesAxiom(obj, getEntity(yaml[key].inverseOf, EntityType.OBJECT_PROPERTY)))
                }
                addAnnotation(yaml[key], key)
            }
            else if (yaml[key].type) {
                makeIndividual(yaml[key], key)
            }

            // Classes
            else {
                if (yaml[key].is_a) {
                    def sup = getEntity(yaml[key].is_a, EntityType.CLASS)
                    manager.addAxiom(onto, factory.getOWLSubClassOfAxiom(getEntity(key, EntityType.CLASS), sup))
                } else{
                    manager.addAxiom(onto, factory.getOWLDeclarationAxiom(getEntity(key, EntityType.CLASS)));
                }
                yaml[key].keySet().forEach{
                    if (it == 'is_a') return
                    def prop = getEntity(it)
                    def value = yaml[key][it]

                    if(!prop) {
                        println "Error: $it = $value."
                        return
                    }

                    if (prop.OWLObjectProperty) {
                        if (value in List)
                            restriction(getEntity(key), prop, value)
                        else {
                            def cls = getEntity(value)
                            if (cls.OWLClass)
                                restriction(getEntity(key), prop, cls)
                            else {
                                println "Error: $key has restriction on property $it refering to no-class entity ${yaml[key][it]}"
                                return
                            }
                        }
                    }
                    else if (prop.OWLDataProperty) {
                        //if (value in List){
                        //    restriction(Class(key), prop, value)
                        //} else {
                        //def dataType =
                        //if (cls.OWLClass){
                        restriction(getEntity(key), prop, getEntity(value))
                        //} else {
                        //    println "Error: $key has restriction on property $it refering to no-class entity ${yaml[key][it]}"
                        //    return
                        //}
                        //}
                    }
                }
                addAnnotation(yaml[key], key)
            }
            //println "Key $key: ${yaml[key]}"
        }
        yaml.keySet().forEach { key ->
            if (key == 'addFrom') {
                yaml[key].forEach { cmd ->
                    // SPARQL 1.0 or 1.1 endpoint
                    def sparql = new Sparql(endpoint: cmd.endpoint) //, user:"user", pass:"pass")

                    String template = cmd.template
                    sparql.eachRow(cmd.query, { Map row ->
                        //println row['uri1']
                        for (String line : template.split('\n')) {
                            //println "line: $line"
                            def elem = line.split(' ')
                            makeIndividual(parseElem(elem[0], row), parseElem(elem[1], row), parseElem(elem[2], row))
                        }
                    })
                }
            }
        }
    }

    static void main(String[] args) {
        String file = 'sustenagro.yaml'
        def format = 'manchester'
        if (args.length > 1)  file = args[0]
        if (args.length > 2)  format = args[1]

        Map yaml = (Map) new Yaml().load(new FileReader(file))

        println "Ontology: ${yaml.ontology}"

        def onto = new Yaml2Owl((String) yaml.ontology)
        onto.format = format

        onto.readYaml(yaml)

        println 'Saving ...'
        if (file.endsWith('.yaml'))
            file = file.substring(0, file.length()-5)

        file = file + '.owl'
        onto.save(file, format)
        println "Saved: $file"
    }
}
