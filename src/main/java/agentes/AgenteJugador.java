/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetResponder;
import jade.proto.ProposeResponder;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.Motivo;
import juegosTablero.aplicacion.OntologiaJuegoConecta4;
import juegosTablero.aplicacion.conecta4.Movimiento;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import juegosTablero.dominio.elementos.Posicion;
import juegosTablero.dominio.elementos.ProponerJuego;
import tareas.TareaBuscarAgentes;
import utilidad.GestorSuscripciones;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class AgenteJugador extends Agent implements Vocabulario{
    
    private Ontology ontology;
	private final ContentManager manager = (ContentManager) getContentManager();
	private Jugador jugador;
    private Random rand;
    private int estadoTablero[][];
    
    /**
     * Inicializacion del Agente y las tareas iniciales.
     */
    @Override
    protected void setup() {
        
        //Incialización de variables
		jugador = new Jugador(this.getLocalName(), this.getAID());
        rand = new Random();
        
        //Regisro de la Ontología
        try {
            ontology = OntologiaJuegoConecta4.getInstance();
        } catch (BeanOntologyException e) {
            e.printStackTrace();
        }
        
        //Registro en Página Amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType(tipoServicio);
		sd.setName(NombreServicio.JUEGO_CONNECTA_4.name());
		dfd.addServices(sd);
		try {
				DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
				fe.printStackTrace();
		}
        
        //Se añaden las tareas principales.
        MessageTemplate temp = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
		addBehaviour(new TareaRecepcionProposicionJuego(this, temp));
	}
    
    /**
     * Finalzacion del Agente.
     */
    @Override
    protected void takeDown() {

        //Desregistro de las Páginas Amarillas
        try {
            DFService.deregister(this);
	}
            catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        //Se liberan los recuros y se despide
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
    
//    private Posicion calcularJugada(){
//        int columna = rand.nextInt(7); //Modificar por el caclculo de la posicion.
//        Posicion pos = new Posicion(columna, getFila(columna));
//        return pos;
//    }
    
//    public int getFila(int columna){
//        for(int i=0; i<6; i++){
//            if(estadoTablero[6-i-1][columna] == CASILLA_VACIA){
//                return i;
//            }
//        }
//        return -1;
//    }
    
    //Tareas del Agente Jugador de Conecta 4.
    
    /**
     * Tarea que recibe la propuesta de jugar de la Central de Juegos (Protocolo Request).
     */
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
						pj = (ProponerJuego) manager.extractContent(propose);
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
    
//    /**
//     * Tarea que indica las jugadas al tablero (Contract Net).
//     */
//    public class TareaJugarPartidaJugador extends ContractNetResponder{
//
//        /**
//         * Constructor de la tarea.
//         * @param a Agente que invoco la tarea.
//         * @param mt Mensaje que se espera recibir.
//         */
//        public TareaJugarPartidaJugador(Agent a, MessageTemplate mt) {
//            super(a, mt);
//        }
//
//        /**
//         * Funcion que procesa un mensaje CFP y acepta o rechaza la propuesta.
//         * @param cfp Mensaje que se ha recibido.
//         * @return 
//         * @throws RefuseException
//         * @throws FailureException
//         * @throws NotUnderstoodException
//         */
//        @Override
//        protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
//            //Se realiza el movimiento
//            //Movimiento movimiento = new Movimiento(calcularJugada(), myAgent.miColor);
//        }
//        
//        /**
//         * 
//         * @param cfp 
//         * @param propose 
//         * @param accept 
//         * @return 
//         * @throws FailureException
//         */
//        @Override
//        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
//            //Comprobar si se continua o se ha ganado la partida
//        }    
//        
//    }
    
}
