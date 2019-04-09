/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author ROBERTO
 */
public class Conecta4JFrame extends JFrame{
    
    public static final int NUM_COLUMNAS = 7;
    public static final int NUM_FILAS = 6;
    
    public static final int CASILLA_VACIA = 0;
    public static final int CASILLA_J1 = 1;
    public static final int CASILLA_J2 = 2;
    
    private JPanel tablero[][];
    private int estadoTablero[][];
//    private Casilla casillas[];
    private ImageIcon casillas[];
    
    public Conecta4JFrame(){
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
                    estadoTablero[i][j] = CASILLA_VACIA;
                }
            }
        casillas = new ImageIcon[3];
        casillas[CASILLA_VACIA] = new ImageIcon("src/main/java/graficos/casilla_vacia.png");
        casillas[CASILLA_J1] = new ImageIcon("src/main/java/graficos/casilla_j1.png");
        casillas[CASILLA_J2] = new ImageIcon("src/main/java/graficos/casilla_j2.png");
        iniciarTablero();
        visualizarTablero();
    }
    
    public void iniciarTablero(){
        for(int i=0; i<NUM_FILAS; i++){
            for(int j=0; j<NUM_COLUMNAS; j++){
                tablero[i][j].removeAll();
                tablero[i][j].add(new JLabel(casillas[CASILLA_VACIA]));
                tablero[i][j].repaint();
                estadoTablero[i][j] = CASILLA_VACIA;
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
    
    public void instertarFicha(int jugador, int columna, int fila){
//        int fila = getFila(columna);
        tablero[NUM_FILAS-fila-1][columna].removeAll();
        tablero[NUM_FILAS-fila-1][columna].add(new JLabel(casillas[jugador]));
        tablero[NUM_FILAS-fila-1][columna].repaint();
        visualizarTablero();
    }
    
//    public int getFila(int columna){
//        for(int i=0; i<NUM_CASILLAS_ALTO; i++){
//            if(estadoTablero[columna][i] == CASILLA_VACIA){
//                return i;
//            }
//        }
//        return -1;
//    }
    
}
