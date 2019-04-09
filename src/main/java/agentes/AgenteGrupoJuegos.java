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
import jade.core.behaviours.TickerBehaviour;
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
import jade.proto.SubscriptionResponder;
import java.util.Iterator;
import java.util.Random;
import juegosTablero.Vocabulario;
import static juegosTablero.Vocabulario.tipoServicio;
import juegosTablero.aplicacion.OntologiaJuegoConecta4;
import juegosTablero.dominio.elementos.CompletarJuego;
import juegosTablero.dominio.elementos.Grupo;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Motivacion;
import juegosTablero.dominio.elementos.ProponerJuego;
import utilidad.GestorSuscripciones;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class AgenteGrupoJuegos extends Agent implements Vocabulario{
    
	private Ontology ontology;
	private final ContentManager manager = (ContentManager) getContentManager();
    private GestorSuscripciones gestorSuscripciones;
	private Grupo grupo;
    private Random rand;
    
    /**
     * Inicializacion del Agente y las tareas iniciales.
     */
    @Override
    protected void setup() {
        
        //Incialización de variables
		grupo = new Grupo(this.getLocalName(), this.getAID());
        gestorSuscripciones = new GestorSuscripciones();
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
		sd.setName(NombreServicio.GRUPO_JUEGOS.name());
		dfd.addServices(sd);
		try {
				DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
				fe.printStackTrace();
		}
        
        //Se añaden las tareas principales.
        MessageTemplate temp = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
		addBehaviour(new TareaRecepcionCompletarJuego(this, temp));
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

    //Tareas del Agente Grupo de Juegos.
    
    /**
     * Tarea que recibe la propuesta de jugar de la Central de Juegos (Protocolo Request).
     */
    public class TareaRecepcionCompletarJuego extends ProposeResponder{
        
        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea. 
         * @param mt Mensaje que se espera recibir.
         */
        public TareaRecepcionCompletarJuego(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException, RefuseException {
            if(rand.nextBoolean()){
                CompletarJuego cj = new CompletarJuego(); 
				try {
					cj = (CompletarJuego) manager.extractContent(propose);
				} catch (Codec.CodecException | OntologyException e) {
					e.printStackTrace();
				}
                ACLMessage accept = propose.createReply();
                accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                accept.setContent(new JuegoAceptado(cj.getJuego(), grupo).toString());
                return accept; 
            }else{
                Motivacion motivacion = new Motivacion(Motivo.PARTICIPACION_EN_JUEGOS_SUPERADA);
                ACLMessage reject = propose.createReply();
                reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                reject.setContent(motivacion.toString());
                return reject;
            }
            //throw new NotUnderstoodException("Remitente desconocido.\n");
        }
        
    }
    
    /**
     * Tarea que recibe y acepta la suscripcion de la Central de Juegos y envia la informacion de las partidas (Subscribe).
     */
    public class TareaInformarJuego extends SubscriptionResponder{
        
        private GestorSuscripciones gestorSub; //Gestor de las suscripciones aceptadas por el agente.
        
        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea. 
         * @param mt Mensaje que se espera recibir.
         * @param sm Gestor de las suscripciones recibidas.
         */
        public TareaInformarJuego(Agent a, MessageTemplate mt, GestorSuscripciones sm) {
            super(a, mt);
            gestorSub = sm;
        }

        /**
         * Funcion que procesa un mensaje de suscripcion.
         * @param subscription Mensaje que solicita la suscripcion.
         * @return Mensaje de aceptacion.
         * @throws NotUnderstoodException
         * @throws RefuseException
         */
        @Override
        protected ACLMessage handleSubscription(ACLMessage subscription) throws NotUnderstoodException, RefuseException {
            //Se añade la suscripcion al gestor del Agente.
            try{
                this.gestorSub.register(this.createSubscription(subscription));
            }catch(Exception e){
                System.out.print("No se ha podido llevar a cabo la suscripcion.\n");
            }
            //Se ecrea y envia el mensaje de aceptacion al agente que realiza la peticion.
            ACLMessage agree = subscription.createReply();
            agree.setPerformative(ACLMessage.AGREE);
            return agree;
        } 
        
    }
        
    /**
     * Tarea que transimte la información del Mercado al Monitor.
     */
    public class TareaEnvioInforme extends TickerBehaviour{

        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea.
         * @param period Periodo que transcurre entre ejecuciones de la tarea.
         */
        public TareaEnvioInforme(Agent a, long period) {
            super(a, period);
        }

        /**
         * Se construye y envia el mensaje con informacion para el ranking al Monitor.
         */
        @Override
        public void onTick() {
            ACLMessage respuesta = new ACLMessage(ACLMessage.INFORM);
            //respuesta.setContent());
            for(Iterator<SubscriptionResponder.Subscription> it = gestorSuscripciones.iterator();it.hasNext();){
                it.next().notify(respuesta);
            }
        }
    }
    
}