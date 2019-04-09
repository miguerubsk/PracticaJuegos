/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Agentes;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.ModoJuego;
import juegosTablero.Vocabulario.NombreServicio;
import juegosTablero.Vocabulario.TipoJuego;
import static juegosTablero.Vocabulario.tipoServicio;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.barcos.JuegoBarcos;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.ProponerJuego;

/**
 *
 * @author pedroj
 */
public class AgenteJugadorBarquitos extends Agent implements Vocabulario{
    private Ontology ontologiaBarcos;
    
    private final Codec codec = new SLCodec();
    private final ContentManager managerBarcos = (ContentManager) getContentManager();
    

    @Override
    protected void setup() {
        System.out.println("Inicia la ejecución de " + this.getName());
        
        try {
            ontologiaBarcos = OntologiaJuegoBarcos.getInstance();
        } catch (BeanOntologyException ex) {
            Logger.getLogger(AgenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        managerBarcos.registerLanguage(codec);
	managerBarcos.registerOntology(ontologiaBarcos);
        
        
        // Registro en las páginas Amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType(NombreServicio.JUEGO_BARCOS.name());
	sd.setName(NombreServicio.GRUPO_JUEGOS.toString());
	dfd.addServices(sd);
	try {
            DFService.register(this, dfd);
	}
	catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        //Juego juego = new Juego("Juego1", 3, ModoJuego.UNICO, TipoJuego.BARCOS);
        //JuegoBarcos juegoBarcos = new JuegoBarcos();
        //ProponerJuego proponerJuego = new ProponerJuego(juego, juegoBarcos);
        
        //Juego juego = new Juego("Juego1", 3, ModoJuego.UNICO, TipoJuego.CONECTA_4);
        //JuegoConecta4 juegoConecta4 = new JuegoConecta4();
        //ProponerJuego proponerJuego = new ProponerJuego(juego, juegoConecta4);
        
        Juego juego = new Juego("Juego", 3, ModoJuego.UNICO, TipoJuego.BARCOS);
        JuegoBarcos juegoBarcos = new JuegoBarcos();
        ProponerJuego proponerJuego = new ProponerJuego(juego, juegoBarcos);        
        
        ACLMessage msg;
        
        msg = new ACLMessage(ACLMessage.PROPOSE);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        msg.setSender(this.getAID());
        msg.setLanguage(codec.getName());
        msg.setOntology(ontologiaBarcos.getName());
        msg.addReceiver(this.getAID());
        
        Action ac = new Action(this.getAID(), proponerJuego);
        
        
        System.out.println(msg);
        
        try {
            // Prueba extracción del mensage

            ac = (Action) managerBarcos.extractContent(msg);
            ProponerJuego juegoPropuesto = (ProponerJuego) ac.getAction();
            System.out.println("-------------");
            System.out.println(juegoPropuesto);
        } catch (Codec.CodecException | OntologyException ex) {
            Logger.getLogger(AgenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }

    @Override
    protected void takeDown() {
        //Desregistro de las Páginas Amarillas
        try {
            DFService.deregister(this);
	}
            catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
}