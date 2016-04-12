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

import groovy.sparql.*
import groovy.transform.CompileStatic
import org.semanticweb.owlapi.model.IRI
import org.w3c.dom.Node

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

@CompileStatic
class LattesReader extends Onto {

    //	def makeKind() {
    def kind = [
            ['//ARTIGO-PUBLICADO', { iri -> Declaration(NamedIndividual(iri)); ClassAssertion('bibo:AcademicArticle', iri) }],
            ['//TRABALHO-EM-EVENTOS', { iri -> Declaration(NamedIndividual(iri)); ClassAssertion('bibo:AcademicArticle', iri) }],
            ['//LIVRO-PUBLICADO', { iri -> Declaration(NamedIndividual(iri)); ClassAssertion('bibo:Book', iri) }],
            ['//CAPITULO-DE-LIVRO-PUBLICADO', { iri -> Declaration(NamedIndividual(iri)); ClassAssertion('bibo:Chapter', iri) }],
            ['//TEXTO-EM-JORNAL-OU-REVISTA[DADOS-BASICOS-DO-TEXTO/@NATUREZA="JORNAL_DE_NOTICIAS"]', { iri -> Declaration(NamedIndividual(iri)); ClassAssertion('bibo:Article', iri) }],
            ['//TEXTO-EM-JORNAL-OU-REVISTA[DADOS-BASICOS-DO-TEXTO/@NATUREZA="REVISTA_MAGAZINE"]', { iri -> Declaration(NamedIndividual(iri)); ClassAssertion('bibo:Article', iri) }],
    ]
    //	}

    def clean(String str) {
        //URI.create(str.trim().replace(' ','_')).toString();
        URLEncoder.encode(str.trim().replace(' ', '_'), 'UTF-8')
        //str.trim().replace(' ','_').replace("'",'').replace(":",'_').replace(",",'').replace("&",'_')
    }

    def findByName(String name) {
        if (name.contains(',')) {
            def aux = name.split(',')
            name = aux[1] + ' ' + aux[0]
        }
        def id = clean(name)
        Declaration(NamedIndividual(id));
        //ClassAssertion('foaf:Person', id)
        def names = name.split()
        DataPropertyAssertion('foaf:lastName', id, names[names.length - 1], '')
        DataPropertyAssertion('foaf:firstName', id, names[0], '')
        AnnotationAssertion('foaf:name', id, name, '')
        //AnnotationAssertion('rdfs:label', id, name, '')
        id
    }

    def findConfByName(String name) {
        def id = clean(name)
        Declaration(NamedIndividual(id));
        ClassAssertion('bibo:Conference', id)
        AnnotationAssertion('foaf:name', id, name, '')
        id
    }

    def findJournalByName(String name) {
        def id = clean(name)
        Declaration(NamedIndividual(id));
        ClassAssertion('bibo:Journal', id)
        AnnotationAssertion('dc:title', id, name, '')
        id
    }

    def findBookByName(String name) {
        def id = clean(name)
        Declaration(NamedIndividual(id));
        ClassAssertion('bibo:Book', id)
        AnnotationAssertion('dc:title', id, name, '')
        id
    }


    def conv = [
            ['.//@NOME-COMPLETO-DO-AUTOR', { subject, String text -> AnnotationAssertion('dc:creator', subject, findByName(text)) }],
            ['.//@NOME-DO-EVENTO', { subject, text -> AnnotationAssertion('bibo:presentedAt', subject, findConfByName((String) text)) }],

            ['.//@TITULO-DO-LIVRO', { subject, text -> ObjectPropertyAssertion('dc:isPartOf', subject, findBookByName((String) text)) }],
            //		['%C ', './/@LOCAL-DE-PUBLICACAO'],
            //		['%C ', './/@CIDADE-DA-EDITORA'],
            //		['%D ', './/@ANO-DO-ARTIGO'],
            //		['%D ', './/@ANO-DO-TRABALHO'],
            //		['%D ', './/@ANO'],
            //		['%D ', './/@ANO-DO-TEXTO'],
            //		['%E ', './/@ORGANIZADORES'],
            //		['%G ', './/@IDIOMA'],
            //		['%I ', './/@NOME-DA-EDITORA'],
            ['.//@TITULO-DO-PERIODICO-OU-REVISTA', { subject, text -> AnnotationAssertion('dc:isPartOf', subject, findJournalByName((String) text)) }],
            ['.//@TITULO-DO-JORNAL-OU-REVISTA', { subject, text -> AnnotationAssertion('dc:title', subject, text, '') }],
            //		['%K ', './/PALAVRAS-CHAVE/@*'],
            //		['%N ', './/@FASCICULO'],
            ['.//@PAGINA-INICIAL', { subject, text -> DataPropertyAssertion('bibo:pageStart', subject, text, 'xsd:integer') }],
            ['.//@PAGINA-FINAL', { subject, text -> DataPropertyAssertion('bibo:pageEnd', subject, text, 'xsd:integer') }],
            ['.//@VOLUME', { subject, text -> DataPropertyAssertion('bibo:volume', subject, text, 'xsd:integer') }],
            //		['%P ', './/@PAGINA-INICIAL', '-', './/@PAGINA-FINAL'],
            //		['%Q ', './/@TITULO-DO-ARTIGO-INGLES'],
            //		['%Q ', './/@TITULO-DO-TRABALHO-INGLES'],
            //		['%Q ', './/@TITULO-DO-CAPITULO-DO-LIVRO-INGLES'],
            //		['%Q ', './/@TITULO-DO-TEXTO-INGLES'],
            //		['%R ', './/@DOI'],

            ['.//@TITULO-DO-ARTIGO', { subject, text -> AnnotationAssertion('dc:title', subject, text, '') }],
            ['.//@TITULO-DO-TRABALHO', { subject, text -> AnnotationAssertion('dc:title', subject, text, '') }],
            ['.//@TITULO-DO-CAPITULO-DO-LIVRO', { subject, text -> AnnotationAssertion('dc:title', subject, text, '') }],
            ['.//@TITULO-DO-TEXTO', { subject, text -> AnnotationAssertion('dc:title', subject, text, '') }],

            //		['%U ', './/@HOME-PAGE-DO-TRABALHO'],
            //		['%V ', './/@VOLUME'],
            //		// It's last to avoid problematic characters inside the notes
            //		// Commented out as it can generate new lines
            //		//['%Z ', './/@DESCRICAO-INFORMACOES-ADICIONAIS'],
            //		['%6 ', './/@NUMERO-DE-VOLUMES'],
            //		['%7 ', './/@NUMERO-DA-EDICAO-REVISAO'],
            //		['%8 ', './/@DATA-DE-PUBLICACAO'],
            //		['%@ ', './/@ISSN'],
            //		['%@ ', './/@ISBN']
    ]

    //	def inPeriod(XPath xpath, Node pub){
    //		try {
    //			def cmd = './/@ANO-DO-ARTIGO | .//@ANO-DO-TRABALHO | .//@ANO | .//@ANO-DO-TEXTO'
    //			def yearStr = ((NodeList) xpath.evaluate(cmd, pub, XPathConstants.NODESET)).item(0).nodeValue
    //
    //			def year = Integer.parseInt(yearStr)
    //			(year >= year1) && (year <= year2)
    //		}
    //		catch (e) {false}
    //	}

    //@TypeChecked(SKIP)
    def path(String fileName) {

        def domFactory = DocumentBuilderFactory.newInstance()
        domFactory.namespaceAware = true
        def doc = domFactory.newDocumentBuilder().parse(fileName)
        def xpath = XPathFactory.newInstance().newXPath()

        int journal = 0;

        //new File(fileOutput).withWriter { out ->

        kind.each { List type ->
            xpath.evaluate((String) type[0], doc, XPathConstants.NODESET).each { Node pub ->
                //if (!inPeriod(xpath, pub)) return;
                //out.writeLine(type[0])
                def indiv = ':journal' + journal
                ((Closure) type[1])(indiv)
                journal++

                conv.each { List cmd ->
                    xpath.evaluate((String) cmd[0], pub, XPathConstants.NODESET).each { Node node ->
                        if (node.nodeValue == '') return;
                        //out.write(cmd[0] + node.nodeValue)
                        ((Closure) cmd[1])(indiv, node.nodeValue)
                        //print cmd[0] + '--'+ node.nodeValue+'\n'

                        // Two columns command?
                        //if (cmd.size()>2)
                        //	xpath.evaluate(cmd[3], pub, XPathConstants.NODESET).each{ Node it ->
                        //		out.write(cmd[2] + it.nodeValue)
                        //	}
                        //out.write('\n')
                    }
                }
                //out.write('\n')
            }
        }
    }

    /**
     * Read a person from a Lattes.xml file
     * @param collection
     * @return
     */
    def readPerson(String lattes) {
        def ontoURI = IRI.create(ClassLoader.getSystemResource('people.owl'))
        loadOntology(ontoURI)

        Prefix('http://dilvan.org/onto/people/')
        Prefix('dc:', 'http://purl.org/dc/elements/1.1/')
        Prefix('foaf:', 'http://xmlns.com/foaf/0.1/')
        Prefix('bibo:', 'http://purl.org/ontology/bibo/')

        Declaration(Class(':TaxonRef'))
        Declaration(Class('foaf:TaxonRef2'))

        path(ClassLoader.getSystemResource('curriculo.xml').toString())

        println 'l'

        //onto.path(ClassLoader.getSystemResource('curriculo.xml').toString())

        def out = ontoURI.toString().replace('people.owl', 'people.out.owl')
        println out
        save(IRI.create(out))
    }

    static main(args) {
        // SPARQL 1.0 or 1.1 endpoint
        //	def sparql = new Sparql(endpoint:"http://localhost:1234/testdb/query", user:"user", pass:"pass")
        //
        //	def query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 4"
        //
        //	// sparql result variables projected into the closure delegate
        //	sparql.each query, {
        //	    println "${s} : ${p} : ${o}"
        //	}

        def onto = new LattesReader()

        onto.readPerson('curriculo.xml')
    }

}
