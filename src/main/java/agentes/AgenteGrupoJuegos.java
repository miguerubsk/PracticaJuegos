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
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
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
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.proto.ProposeResponder;
import jade.proto.SubscriptionResponder;
import jade.util.leap.List;
import jade.wrapper.StaleProxyException;
import static java.lang.Math.ceil;
import static java.lang.Math.log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.OntologiaJuegoConecta4;
import juegosTablero.dominio.elementos.ClasificacionJuego;
import juegosTablero.dominio.elementos.CompletarJuego;
import juegosTablero.dominio.elementos.Grupo;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import utilidad.GestorSuscripciones;
import utilidad.Participante;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class AgenteGrupoJuegos extends Agent implements Vocabulario{
    
    public static final int NUM_JUEGOS_IMPLEMENTADOS = 2;
    public static final int ONTOLOGIA_BARCOS = 0;
    public static final int ONTOLOGIA_CONECTA4 = 1;
    public static final int PERIODO_ENVIO = 10000;
    
    // Para la generación y obtención del contenido de los mensages
    private ContentManager manager[];
	
    // El lenguaje utilizado por el agente para la comunicación es SL 
    private final Codec codec = new SLCodec();

    // Las ontología que utilizará el agente
    private Ontology ontologias[];
    
    private GestorSuscripciones gestorSuscripciones;
    private Grupo grupo;
    private CompletarJuego juego;
    private ArrayList<Jugador> jugadores;
    private ArrayList<Integer> puntuaciones;
    private ArrayList<AID> tablerosActivos;
    private int numRondas;
    private ArrayList<Participante> participantes;
    private int ronda;
    private int partida;
    private boolean emparejamientos[][];
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
        jugadores = new ArrayList<>();
        tablerosActivos = new ArrayList<>();
        numRondas = 1; //En caso de juego unico, no se calculan las rondas.
        participantes = new ArrayList<>();
        partida = 0;
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
	sd.setType(TIPO_SERVICIO);
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
        MessageTemplate temp2 = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE),MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE));
                
        //Se añaden las tareas principales.
        addBehaviour(new TareaRecepcionCompletarJuego(this, temp));
        addBehaviour(new TareaInformarJuego(this, temp2, gestorSuscripciones));
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

    //Funciones para organizar el torneo suizo.
    
    /**
     * Algoritmo burbuja que ordena los Participantes por puntuacion.
     * @param participantes Array con los participantes del torneo.
     */
    static public void Burbuja(ArrayList<Participante> participantes){
        Participante aux;
        boolean fin = false;
        while(!fin){
            fin = true;
            for(int i=0; i<participantes.size()-1; i++){
                if(participantes.get(i).getPuntos() < participantes.get(i+1).getPuntos()){
                    aux = participantes.get(i);
                    participantes.set(i, participantes.get(i+1));
                    participantes.set(i+1, aux);
                    fin = false;
                }
            }  
        }
    }
    
    /**
     * Funcion que devuelve el numero de rondas que se jugaran en el torneo.
     * @param numParticipantes Numero de participantes del torneo.
     * @return Numero de rondas a jugar.
     */
    static public int calcularRondas(int numParticipantes){
        return (int)ceil(log(numParticipantes)/log(2));
    }
    
    /**
     * Funcion que devuelva el emparejamiento para una partida
     * @param j1 Primer jugador de la partida.
     * @param j2 Segundo jugador de la partida.
     * @return ArrayList con la pareja de jugadores.
     */
    public ArrayList<Jugador> addPartida(Jugador j1, Jugador j2){
        ArrayList<Jugador> emparejamento = new ArrayList<>();
        emparejamento.add(j1);
        emparejamento.add(j2);
        return emparejamento;
    }
    
    public void jugarRonda(ArrayList<ArrayList<Jugador>> partidas){
        if(juego.getJuego().getTipoJuego() == TipoJuego.BARCOS){
            //Se crea el tablero para los Barquitos.
    //                    try {
    //                        this.getContainerController().createNewAgent(NOMBRE_AGENTE_IMPAR, "agentes.AgenteLadronImpar", null).start();
    //                    }catch(StaleProxyException ex) {
    //                       // Logger.getLogger(AgentePolicia.class.getName()).log(Level.SEVERE, null, ex);
    //                    }
        }else{
            //Se crea el tablero para el Conecta 4.
            try {
                for(int i=0; i<partidas.size(); i++){
                    partida++;
                    Object[] args = new Object[4];
                    args[0] = juego;
                    args[1] = partida;
                    args[2] = partidas.get(i);
                    args[3] = this.getAID();
                    String nombreTablero = "TableroConecta4_"+juego.getJuego().getIdJuego()+"_"+partida;
                    getContainerController().createNewAgent(nombreTablero, "agentes.AgenteTableroConecta4", args).start();
                    //Se registra el tablero en los tableros activos.
                    tablerosActivos.add(new AID(nombreTablero, AID.ISLOCALNAME));
                }
            }catch(StaleProxyException e) {
                Logger.getLogger(AgenteGrupoJuegos.class.getName()).log(Level.SEVERE, null, e);
            }
            
            //Se prepara la plantilla con la peticion para todos los agentes.
            ACLMessage mensaje = new ACLMessage(ACLMessage.REQUEST);
            mensaje.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            mensaje.setSender(this.getAID());
            for(int i=0; i<tablerosActivos.size(); i++){
                mensaje.addReceiver(tablerosActivos.get(i));
            }
            tablerosActivos.clear();
            mensaje.setLanguage(codec.getName());
            if(juego.getJuego().getTipoJuego() == TipoJuego.BARCOS){
                mensaje.setOntology(ontologias[ONTOLOGIA_BARCOS].getName());
            }else{
                mensaje.setOntology(ontologias[ONTOLOGIA_CONECTA4].getName());
            }
            
            //Se inicia la tarea de peticion de puntuaciones.
            addBehaviour(new TareaRecibirResultados(this,mensaje));
        }
    }
    
    /**
     * Funcion para organizar una ronda intermedia.
     * @param participantes Array con los participantes del torneo.
     */
    public void organizarRonda(ArrayList<Participante> participantes){

        //Vector que lleva la cuenta de los participantes asignados a alguna partida.
        boolean adjudicados[] = new boolean[participantes.size()];
        for(int i=0; i<participantes.size(); i++){
            adjudicados[i] = false;
        }
        
        //Se organizan los grupos.
        int numGrupos = 0, valAct = -1;
        ArrayList<ArrayList<Participante>> grupos = new ArrayList<>();
        for(int i=0; i<participantes.size(); i++){
            if(valAct != participantes.get(i).getValor()){
                numGrupos++;
                grupos.add(new ArrayList<>());
            }
            grupos.get(numGrupos-1).add(participantes.get(i));
            valAct = participantes.get(i).getValor();
        }
        
        //Si son impares en algun grupo, el ultimo participante se pasa al siguiente grupo.
        for(int i=0; i<numGrupos-1; i++){
            if(grupos.get(i).size()%2 != 0){
                grupos.get(i+1).add(grupos.get(i).remove(grupos.get(i).size()-1));
            }
        }
        
        //Si los participantes son impares, se selecciona al ultimo que no haya tenido una ronda libre (BYE).
        if(participantes.size()%2 != 0){
            boolean libre = false;
            int index = participantes.size();
            while(!libre){
                index--;
                if(!emparejamientos[participantes.get(index).getId()][participantes.size()]){
                    participantes.get(index).BYE();
                    System.out.println(participantes.get(index).getJugador().getNombre()+": Ronda Libre. \n");
                    adjudicados[participantes.get(index).getId()] = true;
                    emparejamientos[participantes.get(index).getId()][participantes.size()] = true;
                    libre = true;
                }
            }
        }
 
        //Array que almacena los emparejamientos de la ronda.
        ArrayList<ArrayList<Jugador>> partidas = new ArrayList<>();
        
        //Se emparejan los participantes y se juegan las partidas.
        int index;
        for(int i=0; i<numGrupos; i++){
            for(int j=0; j<grupos.get(i).size(); j++){
                if(!adjudicados[grupos.get(i).get(j).getId()]){
                    do{
                        index = rand.nextInt(grupos.get(i).size());
                    }while(adjudicados[grupos.get(i).get(index).getId()] || grupos.get(i).get(index).getId()==grupos.get(i).get(j).getId() || emparejamientos[grupos.get(i).get(j).getId()][grupos.get(i).get(index).getId()]);
                    partidas.add(addPartida(grupos.get(i).get(j).getJugador(), grupos.get(i).get(index).getJugador()));
                    emparejamientos[grupos.get(i).get(j).getId()][grupos.get(i).get(index).getId()] = true;
                    emparejamientos[grupos.get(i).get(index).getId()][grupos.get(i).get(j).getId()] = true;
                    adjudicados[grupos.get(i).get(j).getId()] = true;
                    adjudicados[grupos.get(i).get(index).getId()] = true;
                }
            }
        }
        jugarRonda(partidas);
    }

    /**
     * Funcion para realizar los emparejamientos de la ultima ronda.
     * @param participantes Array con los participantes del torneo.
     */
    public void organizarUltimaRonda(ArrayList<Participante> participantes){
        
        //Vector que lleva la cuenta de los participantes asignados a alguna partida.
        boolean adjudicados[] = new boolean[participantes.size()];
        for(int i=0; i<participantes.size(); i++){
            adjudicados[i] = false;
        }
        
        //Si los participantes son impares, se selecciona al ultimo que no haya tenido una ronda libre (BYE).
        if(participantes.size()%2 != 0){
            boolean libre = false;
            int index = participantes.size();
            while(!libre){
                index--;
                if(!emparejamientos[participantes.get(index).getId()][participantes.size()]){
                    participantes.get(index).BYE();
                    System.out.println(participantes.get(index).getJugador().getNombre()+": Ronda Libre. \n");
                    adjudicados[index] = true;
                    emparejamientos[participantes.get(index).getId()][participantes.size()] = true;
                    libre = true;
                }
            }
        }
        
        //Array que almacena los emparejamientos de la ronda.
        ArrayList<ArrayList<Jugador>> partidas = new ArrayList<>();
        
        //Se emparejan los participantes y se juegan las partidas.
        for(int i=0; i<participantes.size(); i++){
            for(int j=0; j<participantes.size(); j++){
                if(!adjudicados[i] && !adjudicados[j] && !emparejamientos[participantes.get(i).getId()][participantes.get(j).getId()]){
                    partidas.add(addPartida(participantes.get(i).getJugador(), participantes.get(j).getJugador()));
                    emparejamientos[participantes.get(i).getId()][participantes.get(j).getId()] = true;
                    emparejamientos[participantes.get(j).getId()][participantes.get(i).getId()] = true;
                    adjudicados[i] = true;
                    adjudicados[j] = true;
                }
            }
        }
        
        jugarRonda(partidas);
    }
    
    /**
     * Funcion que organiza un torneo por emparejamiento suizo.
     */
    public void torneoSuizo(){
        numRondas = calcularRondas(jugadores.size());
        
        //Matriz que almacena los participantes que han sido emparejados.
        emparejamientos = new boolean[jugadores.size()][jugadores.size()+1];
        for(int i=0; i<jugadores.size(); i++){
            for(int j=0; j<jugadores.size()+1; j++){
                emparejamientos[i][j] = (i == j);
            }
        }
        
        if(ronda != numRondas){
            //Se juega una ronda intermedia.
            System.out.println("Ronda "+ronda+": \n");
            organizarRonda(participantes);
        }else{
            //Se juega la ronda final.
            System.out.println("Ronda "+numRondas+": \n");
            organizarUltimaRonda(participantes);
        }
    }
    
    public void organizarJuego(){
        partida = 0;
        //Se crean los participantes del juego.
        participantes.clear();
        for(int i=0; i<jugadores.size(); i++){
            Participante participante = new Participante(i, jugadores.get(i));
            participantes.add(participante);
        }
        ronda = 1;
        if(juego.getJuego().getModoJuego() == ModoJuego.UNICO){
            ArrayList<ArrayList<Jugador>> rondas = new ArrayList<>();
            rondas.add(jugadores);
            jugarRonda(rondas);
        }else{
            torneoSuizo();
        }
    }
    
    public ClasificacionJuego obtenerClasificacion(){
        List jugadoresClasificacion = new jade.util.leap.ArrayList();
        List puntuacionesClasificacion = new jade.util.leap.ArrayList();
        for(int i=0; i<participantes.size(); i++){
            jugadoresClasificacion.add(participantes.get(i).getJugador());
            puntuacionesClasificacion.add(participantes.get(i).getPuntos());
        }
        return new ClasificacionJuego(juego.getJuego(), jugadoresClasificacion, puntuacionesClasificacion);
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
                juego = (CompletarJuego) ac.getAction();
                jugadores.clear();
                for(Iterator<Jugador> it = juego.getListaJugadores().iterator(); it.hasNext();){
                    jugadores.add(it.next());
                }
                    
                if(juego.getJuego().getTipoJuego() == TipoJuego.CONECTA_4 || juego.getJuego().getTipoJuego() == TipoJuego.BARCOS){
                    organizarJuego();
                    ACLMessage accept = propose.createReply();
                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    manager[ONTOLOGIA_CONECTA4].fillContent(accept, new JuegoAceptado(juego.getJuego(), grupo));
                    return accept; 
                }else{
                    ACLMessage reject = propose.createReply();
                    reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    manager[ONTOLOGIA_BARCOS].fillContent(reject, new Motivacion(juego.getJuego(), Motivo.TIPO_JUEGO_NO_IMPLEMENTADO));
                    return reject;
                }
            } catch (Codec.CodecException | OntologyException e) {
                throw new NotUnderstoodException("Error durante la incializacion del torneo");
            }
            
            //throw new NotUnderstoodException("Remitente desconocido.\n");
        }
        
    }
    
    /**
     * Tarea que recibe los resultados de las partidas de una ronda de los agentes tablero (Protocolo Request).
     */
    public class TareaRecibirResultados extends  AchieveREInitiator{
        
        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea.
         * @param msg Mensaje que se enviara a los agentes receptores.
         */
        public TareaRecibirResultados(Agent a, ACLMessage msg) {
            super(a, msg);
        }
        
        /**
         * Funcion que procesa las respuestas del resto de agentes involucrados en el protocolo. 
         * @param responses Vector con las respuestas de los agentes receptores.
         */
        @Override
        protected void handleAllResponses(Vector responses) {
            for(int m=0; m<responses.size(); m++){
                ACLMessage mensaje = (ACLMessage) responses.get(m);
                if(mensaje.getPerformative() == ACLMessage.AGREE){
                    try{
                        ClasificacionJuego cj = (ClasificacionJuego)  manager[ONTOLOGIA_CONECTA4].extractContent(mensaje);
                        for(int i=0; i<cj.getListaJugadores().size(); i++){
                            for(int j=0; j<participantes.size(); j++){
                                Jugador jugadorAct = (Jugador) cj.getListaJugadores().get(i);
                                if(jugadorAct.getNombre().equals(participantes.get(j).getJugador().getNombre())){
                                    long puntos = (long) cj.getListaPuntuacion().get(i);
                                    participantes.get(j).sumarPuntuacion(puntos);
                                }
                            }
                        }
                        Collections.sort(participantes);
                    } catch (Codec.CodecException | OntologyException e) {
                        Logger.getLogger(AgenteGrupoJuegos.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            }
            
            ronda++;
            if(ronda > numRondas){
                //Se obtiene la clasificacion final.
                Burbuja(participantes);
                System.out.println("Resultados del Torneo: ");
                for(int i=0; i<participantes.size(); i++){
                    System.out.println("Puesto "+i+": "+participantes.get(i).getJugador().getNombre()+". Puntuacion: "+participantes.get(i).getPuntos()+".");
                }
                addBehaviour(new TareaEnvioInforme());
            }else if(ronda == numRondas){
                //Se juega la ronda final.
                System.out.println("Ronda "+ronda+": \n");
                organizarUltimaRonda(participantes);
            }else{
                //Se juega otra ronda intermedia.
                System.out.println("Ronda "+ronda+": \n");
                organizarRonda(participantes);
            }
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
    public class TareaEnvioInforme extends OneShotBehaviour{

        /**
         * Constructor de la tarea.
         */
        public TareaEnvioInforme() {
            super();
        }

        /**
         * Se construye y envia el mensaje con informacion para el ranking al Monitor.
         */
        @Override
        public void action() {
            ACLMessage respuesta = new ACLMessage(ACLMessage.INFORM);
            respuesta.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            respuesta.setSender(myAgent.getAID());
            respuesta.setLanguage(codec.getName());
            respuesta.setOntology(ontologias[ONTOLOGIA_CONECTA4].getName());
            try{
                manager[ONTOLOGIA_CONECTA4].fillContent(respuesta, obtenerClasificacion());
            }catch(Codec.CodecException | OntologyException e) {
                Logger.getLogger(AgenteGrupoJuegos.class.getName()).log(Level.SEVERE, null, e);
            }
            for(Iterator<SubscriptionResponder.Subscription> it = gestorSuscripciones.iterator();it.hasNext();){
                it.next().notify(respuesta);
            }
        }
    }
    
}