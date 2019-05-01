package GUI;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import juegosTablero.Vocabulario;
import juegosTablero.aplicacion.barcos.PosicionBarcos;
import juegosTablero.dominio.elementos.Posicion;

/**
 *
 * @author ROBERTO
 */
public class BarquitosJFrame extends JFrame{
    
    public static final int NUM_COLUMNAS = 10;
    public static final int NUM_FILAS = 10;
    
    public static final int AGUA = 0;
    public static final int BARCO = 1;
    public static final int TOCADO = 2;
    
    private JPanel tablero[][];
    private int estadoTablero[][];
//    private Casilla casillas[];
    private ImageIcon casillas[];
    
    public BarquitosJFrame(){
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        tablero = new JPanel[NUM_FILAS][NUM_COLUMNAS];
        for(int i=0; i<NUM_FILAS; i++){
            for(int j=0; j<NUM_COLUMNAS; j++){
                tablero[i][j] = new JPanel();
            }
        }
        estadoTablero = new int[NUM_FILAS][NUM_COLUMNAS];
            for(int i=0; i<NUM_FILAS; i++){
                for(int j=0; j<NUM_COLUMNAS; j++){
                    estadoTablero[i][j] = AGUA;
                }
            }
        casillas = new ImageIcon[3];
        casillas[AGUA] = new ImageIcon("src/main/java/graficos/agua.jpg");
        casillas[BARCO] = new ImageIcon("src/main/java/graficos/barco.jpg");
        casillas[TOCADO] = new ImageIcon("src/main/java/graficos/tocado.jpg");
        iniciarTablero();
        visualizarTablero();
    }
    
    public void iniciarTablero(){
        for(int i=0; i<NUM_FILAS; i++){
            for(int j=0; j<NUM_COLUMNAS; j++){
                tablero[i][j].removeAll();
                tablero[i][j].add(new JLabel(casillas[AGUA]));
                tablero[i][j].repaint();
                estadoTablero[i][j] = AGUA;
            }
        }
    }
    
    public void colocarBarcos(Posicion coor, Vocabulario.Orientacion orientacion, int tam){
        int x = coor.getCoorX();
        int y = coor.getCoorY();
        if (orientacion == Vocabulario.Orientacion.HORIZONTAL){
            for (int i = y; i<= tam; i++){
                estadoTablero[x][i]= BARCO;
            }
        }else{
            for (int i = x; i<= tam; i++){
                estadoTablero[i][y]= BARCO;
            }
        }
    }
    
    public void visualizarTablero(){
        Container container = this.getContentPane();
        container.removeAll();
        JPanel tableroVisual = new JPanel();
        tableroVisual.setLayout(new GridLayout(NUM_FILAS,NUM_COLUMNAS));
        for(int i=0; i<NUM_FILAS; i++){
            for(int j=0; j<NUM_COLUMNAS; j++){
                tableroVisual.add(tablero[i][j]);
            }
        }
        container.add(tableroVisual);
        this.pack();
    }        
}