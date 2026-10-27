package com.deepnote.fx;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HomeViewControllerTest {
    @Test void vectorIsTheDefaultSearchMode(){assertEquals(HomeViewController.SearchMode.VECTOR,new HomeViewController().searchMode());}
}
