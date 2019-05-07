/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
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
import jade.wrapper.StaleProxyException;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.OntologiaJuegoConecta4;
import juegosTablero.dominio.elementos.CompletarJuego;
import juegosTablero.dominio.elementos.Grupo;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Motivacion;
import utilidad.GestorSuscripciones;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class AgenteGrupoJuegos extends Agent implements Vocabulario{
    
    public static final int NUM_JUEGOS_IMPLEMENTADOS = 2;
    public static final int ONTOLOGIA_BARCOS = 0;
    public static final int ONTOLOGIA_CONECTA4 = 1;
    
    // Para la generación y obtención del contenido de los mensages
    private ContentManager manager[];
	
    // El lenguaje utilizado por el agente para la comunicación es SL 
    private final Codec codec = new SLCodec();

    // Las ontología que utilizará el agente
    private Ontology ontologias[];
    
    private GestorSuscripciones gestorSuscripciones;
    private Grupo grupo;
    private Random rand;
    
    /**
     * Inicializacion del Agente y las tareas iniciales.
     */
    @Override
    protected void setup() {
        
        //Incialización de variables
        manager = new ContentManager[NUM_JUEGOS_IMPLEMENTADOS];
        for(int i=0; i<NUM_JUEGOS_IMPLEMENTADOS; i++){
            manager[i] = (ContentManager) getContentManager();
        }
        ontologias = new Ontology[NUM_JUEGOS_IMPLEMENTADOS];
    	grupo = new Grupo(this.getLocalName(), this.getAID());
        gestorSuscripciones = new GestorSuscripciones();
        rand = new Random();
        
        //Regisro de la Ontología
        try {
            ontologias[ONTOLOGIA_BARCOS] = OntologiaJuegoBarcos.getInstance();
            ontologias[ONTOLOGIA_CONECTA4] = OntologiaJuegoConecta4.getInstance();
        } catch (BeanOntologyException e) {
            e.printStackTrace();
        }
        
        //Registro en Paginas Amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType("GrupoJuegos");
	sd.setName(NombreServicio.GRUPO_JUEGOS.name());
	dfd.addServices(sd);
	try {
            DFService.register(this, dfd);
	}catch(FIPAException fe){
            fe.printStackTrace();
	}
        for(int i=0; i<NUM_JUEGOS_IMPLEMENTADOS; i++){
            manager[i].registerLanguage(codec);
            manager[i].registerOntology(ontologias[i]);
        }
        
        
        //Templates para las tareas de recepcion
        MessageTemplate temp = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
	
        //Se añaden las tareas principales.
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
            
            try {
                Action ac = (Action)  manager[ONTOLOGIA_BARCOS].extractContent(propose);
                CompletarJuego cj = (CompletarJuego) ac.getAction();
                if(cj.getJuego().getTipoJuego() == TipoJuego.CONECTA_4 || cj.getJuego().getTipoJuego() == TipoJuego.BARCOS){
                    if(cj.getJuego().getTipoJuego() == TipoJuego.BARCOS){
                        //Se crea el tablero para los Barquitos.
    //                    try {
    //                        this.getContainerController().createNewAgent(NOMBRE_AGENTE_IMPAR, "agentes.AgenteLadronImpar", null).start();
    //                    }catch(StaleProxyException ex) {
    //                       // Logger.getLogger(AgentePolicia.class.getName()).log(Level.SEVERE, null, ex);
    //                    }
                    }else{
                        //Se crea el tablero para el Conecta 4.
                        try {
                            Object[] args = new Object[1];
                            args[0] = cj;
                            getContainerController().createNewAgent("TableroConecta4", "agentes.AgenteTablero", args).start();
                        }catch(StaleProxyException e) {
                           Logger.getLogger(AgenteGrupoJuegos.class.getName()).log(Level.SEVERE, null, e);
                        }
                    }
                    ACLMessage accept = propose.createReply();
                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    manager[ONTOLOGIA_CONECTA4].fillContent(accept, new JuegoAceptado(cj.getJuego(), grupo));
                    return accept; 
                }else{
                    ACLMessage reject = propose.createReply();
                    reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    manager[ONTOLOGIA_BARCOS].fillContent(reject, new Motivacion(cj.getJuego(), Motivo.TIPO_JUEGO_NO_IMPLEMENTADO));
                    return reject;
                }
            } catch (Codec.CodecException | OntologyException e) {
                throw new NotUnderstoodException("Error durante la incializacion del torneo");
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