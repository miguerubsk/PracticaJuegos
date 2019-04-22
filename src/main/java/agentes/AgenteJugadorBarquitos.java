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
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ProposeResponder;
import java.util.Random;
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
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import juegosTablero.dominio.elementos.ProponerJuego;

/**
 *
 * @author pedroj
 */
public class AgenteJugadorBarquitos extends Agent implements Vocabulario{
    private Ontology ontologiaBarcos;
    private Jugador jugador;
    private Random rand;
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
	//sd.setType();
	sd.setName(NombreServicio.JUEGO_BARCOS.name());
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
        
        MessageTemplate temp = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
		addBehaviour(new TareaRecepcionProposicionJuego(this, temp));
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
    
    public class TareaRecepcionProposicionJuego extends ProposeResponder{
        
        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea. 
         * @param mt Mensaje que se espera recibir.
         */
        public TareaRecepcionProposicionJuego(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException, RefuseException {
//            if("CentralJuegos".equals(propose.getSender().getName())){
                if(rand.nextBoolean()){
					ProponerJuego pj = new ProponerJuego();
					try {
						pj = (ProponerJuego) managerBarcos.extractContent(propose);
					} catch (Codec.CodecException | OntologyException e) {
						e.printStackTrace();
					}
                    ACLMessage accept = propose.createReply();
                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    accept.setContent(new JuegoAceptado(pj.getJuego(), jugador).toString());
                    return accept; 
                }else{
                    Motivacion motivacion = new Motivacion(Motivo.PARTICIPACION_EN_JUEGOS_SUPERADA);
                    ACLMessage reject = propose.createReply();
                    reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    reject.setContent(motivacion.toString());
                    return reject;
                }  
//            }else{
//                throw new NotUnderstoodException("Remitente desconocido.\n");
//            }
        }
        
    }
 }
