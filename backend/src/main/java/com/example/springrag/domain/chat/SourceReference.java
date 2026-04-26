package com.example.springrag.domain.chat;

import java.io.Serializable;

public record SourceReference(String title, String snippet) implements Serializable {
}
